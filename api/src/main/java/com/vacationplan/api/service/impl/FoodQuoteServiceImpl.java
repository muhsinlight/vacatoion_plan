package com.vacationplan.api.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vacationplan.api.config.QuoteRefreshConfig;
import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.FoodBandDto;
import com.vacationplan.api.dto.scraper.FoodSearchResponse;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.service.FoodQuoteService;
import com.vacationplan.api.service.RequestQuotaService;
import com.vacationplan.api.util.FoodPlacesJson;
import com.vacationplan.api.util.ScraperKeys;

@Service
public class FoodQuoteServiceImpl implements FoodQuoteService {

	private static final Logger log = LoggerFactory.getLogger(FoodQuoteServiceImpl.class);
	private static final Duration ESTIMATE_RETRY = Duration.ofMinutes(15);

	private final DestinationRepository destinationRepository;
	private final VacationProperties vacationProperties;
	private final RequestQuotaService requestQuotaService;
	private final RestClient scraperRestClient;
	private final Clock clock;
	private final TransactionOperations transactions;
	private final Executor quoteRefreshExecutor;

	public FoodQuoteServiceImpl(
			DestinationRepository destinationRepository,
			VacationProperties vacationProperties,
			RequestQuotaService requestQuotaService,
			@Qualifier("scraperRestClient") RestClient scraperRestClient,
			Clock clock,
			TransactionOperations transactions,
			@Qualifier(QuoteRefreshConfig.EXECUTOR) Executor quoteRefreshExecutor
	) {
		this.destinationRepository = destinationRepository;
		this.vacationProperties = vacationProperties;
		this.requestQuotaService = requestQuotaService;
		this.scraperRestClient = scraperRestClient;
		this.clock = clock;
		this.transactions = transactions;
		this.quoteRefreshExecutor = quoteRefreshExecutor;
	}

	@Override
	public Result refreshIfStale(String cityKey) {
		RefreshTarget target = transactions.execute(status -> loadRefreshTarget(cityKey));
		if (target.outcome() != null) {
			return target.outcome();
		}
		if (!ScraperKeys.isUsable(vacationProperties.getScraperTriggerKey())) {
			log.warn("Yemek fiyatı atlandı: scraper anahtarı yok");
			return Result.SKIPPED;
		}
		CompletableFuture<Result> pending = new CompletableFuture<>();
		try {
			quoteRefreshExecutor.execute(() -> {
				try {
					pending.complete(fetchFood(cityKey, target));
				} catch (RuntimeException ex) {
					pending.completeExceptionally(ex);
				}
			});
		} catch (RejectedExecutionException ex) {
			log.warn("Yemek listesi kuyruğa alınamadı: {}", cityKey);
			return Result.FAILED;
		}
		try {
			return pending.get(vacationProperties.getFoodRefreshWait().toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException ex) {
			log.warn("Yemek listesi zaman aşımı: {}", cityKey);
			return Result.SCHEDULED;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return Result.FAILED;
		} catch (ExecutionException ex) {
			log.warn("Yemek listesi alınamadı: {}", cityKey);
			return Result.FAILED;
		}
	}

	private Result fetchFood(String cityKey, RefreshTarget target) {
		if (!requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)) {
			log.warn("Yemek fiyatı atlandı: saatlik scraper kotası doldu");
			return Result.FAILED;
		}
		try {
			FoodSearchResponse response = scraperRestClient.post()
					.uri("/food/search")
					.header(ScraperKeys.HEADER, vacationProperties.getScraperTriggerKey())
					.body(new FoodSearchBody(target.displayName()))
					.retrieve()
					.body(FoodSearchResponse.class);
			if (response == null) {
				return Result.FAILED;
			}
			Result stored = transactions.execute(status -> persistFoodQuote(target.destinationId(), response));
			if (stored == Result.FAILED && response.errors() != null && !response.errors().isEmpty()) {
				log.warn("Yemek listesi alınamadı: {} {}", cityKey, response.errors());
			}
			return stored;
		} catch (RestClientException ex) {
			log.warn("Yemek fiyatı alınamadı: {}", cityKey);
			return Result.FAILED;
		}
	}

	private RefreshTarget loadRefreshTarget(String cityKey) {
		Destination destination = destinationRepository.findByCityKey(cityKey).orElse(null);
		if (destination == null || isFresh(destination)) {
			return RefreshTarget.fresh();
		}
		return RefreshTarget.needsRefresh(destination.getId(), destination.getDisplayName());
	}

	private Result persistFoodQuote(Long destinationId, FoodSearchResponse response) {
		Destination destination = destinationRepository.findById(destinationId).orElse(null);
		if (destination == null || !apply(destination, response)) {
			return Result.FAILED;
		}
		return Result.UPDATED;
	}

	private record RefreshTarget(Result outcome, Long destinationId, String displayName) {
		static RefreshTarget fresh() {
			return new RefreshTarget(Result.FRESH, null, null);
		}

		static RefreshTarget needsRefresh(Long destinationId, String displayName) {
			return new RefreshTarget(null, destinationId, displayName);
		}
	}

	private boolean isFresh(Destination destination) {
		Instant checkedAt = destination.getFoodCheckedAt();
		if (checkedAt == null) {
			return false;
		}
		Duration maxAge = destination.getFoodSourceType() == PriceSourceType.OFFICIAL_API
				? vacationProperties.getFoodQuoteMaxAge()
				: ESTIMATE_RETRY;
		return checkedAt.isAfter(clock.instant().minus(maxAge));
	}

	private boolean apply(Destination destination, FoodSearchResponse response) {
		FoodBandDto band = response.band();
		boolean hasBand = band != null && band.min() != null && band.max() != null;
		String placesJson = FoodPlacesJson.write(response.places());
		boolean hasPlaces = !placesJson.equals("[]");
		if (!hasBand && !hasPlaces) {
			destination.setFoodCheckedAt(clock.instant());
			return false;
		}
		if (band != null && band.min() != null && band.max() != null) {
			MoneyRange range = MoneyRange.ofTry(band.min(), band.max());
			destination.setFoodPerPersonPerDayMin(range.min());
			destination.setFoodPerPersonPerDayMax(range.max());
			destination.setFoodSourceType(PriceSourceType.OFFICIAL_API);
		}
		destination.setFoodCheckedAt(clock.instant());
		if (hasPlaces) {
			destination.setFoodPlacesJson(placesJson);
		}
		return true;
	}

	private record FoodSearchBody(String city) {
	}
}
