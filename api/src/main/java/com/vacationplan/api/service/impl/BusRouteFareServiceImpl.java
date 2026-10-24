package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.BusRouteBatchRequest;
import com.vacationplan.api.dto.scraper.BusRouteBatchResponse;
import com.vacationplan.api.dto.scraper.BusScrapeLeg;
import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.BusLegProof;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.ResolvedBusFare;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.repository.RouteQuoteRepository;
import com.vacationplan.api.service.BusRouteFareService;
import com.vacationplan.api.service.RequestQuotaService;
import com.vacationplan.api.util.BusFareMath;
import com.vacationplan.api.util.CityKeyNormalizer;
import com.vacationplan.api.util.ScraperKeys;

@Service
public class BusRouteFareServiceImpl implements BusRouteFareService {

	private static final Logger log = LoggerFactory.getLogger(BusRouteFareServiceImpl.class);

	private final RouteQuoteRepository routeQuoteRepository;
	private final VacationProperties vacationProperties;
	private final RequestQuotaService requestQuotaService;
	private final RestClient scraperRestClient;
	private final Clock clock;

	public BusRouteFareServiceImpl(
			RouteQuoteRepository routeQuoteRepository,
			VacationProperties vacationProperties,
			RequestQuotaService requestQuotaService,
			@Qualifier("scraperRestClient") RestClient scraperRestClient,
			Clock clock
	) {
		this.routeQuoteRepository = routeQuoteRepository;
		this.vacationProperties = vacationProperties;
		this.requestQuotaService = requestQuotaService;
		this.scraperRestClient = scraperRestClient;
		this.clock = clock;
	}

	@Override
	public ResolvedBusFare roundTrip(
			String originCity,
			String destinationCity,
			LocalDate departure,
			LocalDate returning,
			BigDecimal passengers
	) {
		String origin = CityKeyNormalizer.normalize(originCity);
		String dest = CityKeyNormalizer.normalize(destinationCity);
		if (origin.isBlank() || dest.isBlank() || departure == null || returning == null) {
			return new ResolvedBusFare(null, ResolvedBusFare.Status.UNAVAILABLE, null, List.of());
		}
		LocalDate outboundDay = departure;
		LocalDate inboundDay = returning;
		if (!inboundDay.isAfter(outboundDay)) {
			inboundDay = outboundDay.plusDays(2);
		}
		List<RouteQuote> quotes = findPair(origin, dest, outboundDay, inboundDay);
		Optional<RouteQuote> outbound = pick(quotes, origin, dest, BusFareMath.weekBucket(outboundDay));
		Optional<RouteQuote> inbound = pick(quotes, dest, origin, BusFareMath.weekBucket(inboundDay));
		List<String> scrapeErrors = List.of();
		LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
		boolean allowLive = !outboundDay.isBefore(today) && !inboundDay.isBefore(today);
		if (allowLive && (!isFresh(outbound) || !isFresh(inbound))) {
			scrapeErrors = requestLiveFares(origin, dest, outboundDay, inboundDay, outbound, inbound);
			quotes = findPair(origin, dest, outboundDay, inboundDay);
			outbound = pick(quotes, origin, dest, BusFareMath.weekBucket(outboundDay));
			inbound = pick(quotes, dest, origin, BusFareMath.weekBucket(inboundDay));
		}
		if (!isFresh(outbound) || !isFresh(inbound)) {
			return new ResolvedBusFare(null, ResolvedBusFare.Status.UNAVAILABLE, unavailableReason(scrapeErrors), List.of());
		}
		var amount = BusFareMath.roundTrip(outbound.get(), inbound.get(), passengers);
		return new ResolvedBusFare(
				amount,
				ResolvedBusFare.Status.FRESH,
				null,
				List.of(leg("Gidiş", outbound.get()), leg("Dönüş", inbound.get()))
		);
	}

	private static BusLegProof leg(String direction, RouteQuote quote) {
		PriceObservation observation = quote.getObservation();
		String currency = quote.getCurrency() != null ? quote.getCurrency() : "TRY";
		return new BusLegProof(
				direction,
				quote.getMinAmount(),
				currency,
				observation != null ? observation.getProvider() : null,
				observation != null ? observation.getSourceUrl() : null
		);
	}

	private static String unavailableReason(List<String> errors) {
		String joined = String.join(" ", errors).toLowerCase(java.util.Locale.ROOT);
		if (joined.contains("sefer")) {
			return "Obilet bu güzergahta sefer listelemiyor; ulaşım katalog tahmininden yazıldı.";
		}
		if (joined.contains("kota")) {
			return "Obilet çekim kotası doldu; ulaşım katalog tahmininden yazıldı.";
		}
		return "Canlı otobüs fiyatı alınamadı; ulaşım katalog tahmininden yazıldı.";
	}

	private List<String> requestLiveFares(
			String origin,
			String dest,
			LocalDate departure,
			LocalDate returning,
			Optional<RouteQuote> outbound,
			Optional<RouteQuote> inbound
	) {
		if (!ScraperKeys.isUsable(vacationProperties.getScraperTriggerKey())) {
			log.warn("Otobüs fiyatı atlandı: scraper anahtarı yok");
			return List.of("scraper anahtarı yok");
		}
		List<BusScrapeLeg> legs = new ArrayList<>();
		if (!isFresh(outbound)) {
			legs.add(new BusScrapeLeg(origin, dest, departure));
		}
		if (!isFresh(inbound)) {
			legs.add(new BusScrapeLeg(dest, origin, returning));
		}
		if (legs.isEmpty()) {
			return List.of();
		}
		if (!requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)) {
			log.warn("Otobüs fiyatı atlandı: saatlik scraper kotası doldu");
			return List.of("kota doldu");
		}
		try {
			BusRouteBatchResponse response = scraperRestClient.post()
					.uri("/routes/bus")
					.header(ScraperKeys.HEADER, vacationProperties.getScraperTriggerKey())
					.body(new BusRouteBatchRequest(legs))
					.retrieve()
					.body(BusRouteBatchResponse.class);
			if (response == null) {
				return List.of("boş yanıt");
			}
			if (!response.errors().isEmpty()) {
				log.warn("Otobüs fiyatı alınamadı: {}", response.errors());
			}
			return response.errors();
		} catch (RestClientException ex) {
			log.warn("Otobüs fiyatı alınamadı: {} -> {}", origin, dest);
			return List.of("scraper yanıt vermedi");
		}
	}

	private List<RouteQuote> findPair(String origin, String dest, LocalDate departure, LocalDate returning) {
		return routeQuoteRepository.findBusRoundTrip(
				origin,
				dest,
				TransportMode.BUS,
				BusFareMath.weekBucket(departure),
				BusFareMath.weekBucket(returning)
		);
	}

	private static Optional<RouteQuote> pick(List<RouteQuote> quotes, String origin, String dest, LocalDate week) {
		for (RouteQuote quote : quotes) {
			if (origin.equals(quote.getOriginKey())
					&& dest.equals(quote.getDestKey())
					&& week.equals(quote.getWeekBucket())) {
				return Optional.of(quote);
			}
		}
		return Optional.empty();
	}

	private boolean isFresh(Optional<RouteQuote> quote) {
		if (quote.isEmpty()) {
			return false;
		}
		Duration maxAge = vacationProperties.getRouteQuoteMaxAge();
		Instant checkedAt = quote.get().getCheckedAt();
		return checkedAt != null && checkedAt.isAfter(clock.instant().minus(maxAge));
	}
}
