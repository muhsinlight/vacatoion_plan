package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.model.FuelType;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.ResolvedCarFuel;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.FuelPriceRepository;
import com.vacationplan.api.service.CarFuelQuoteService;

@Service
@Transactional(readOnly = true)
public class CarFuelQuoteServiceImpl implements CarFuelQuoteService {

	private static final BigDecimal ECONOMIC_FACTOR = new BigDecimal("0.90");

	private final FuelPriceRepository fuelPriceRepository;
	private final VacationProperties vacationProperties;
	private final Clock clock;

	public CarFuelQuoteServiceImpl(
			FuelPriceRepository fuelPriceRepository,
			VacationProperties vacationProperties,
			Clock clock
	) {
		this.fuelPriceRepository = fuelPriceRepository;
		this.vacationProperties = vacationProperties;
		this.clock = clock;
	}

	@Override
	public ResolvedCarFuel quote(TravelStyle style) {
		Instant cutoff = clock.instant().minus(vacationProperties.getCarFuelMaxAge());
		Optional<BigDecimal> diesel = freshPrice(FuelType.DIESEL, cutoff);
		Optional<BigDecimal> gasoline = freshPrice(FuelType.GASOLINE, cutoff);
		if (diesel.isEmpty() && gasoline.isEmpty()) {
			return new ResolvedCarFuel(
					null,
					ResolvedCarFuel.Status.UNAVAILABLE,
					"Güncel yakıt fiyatı yok; araç bütçesi katalog tahmininden, tek araç olarak yazıldı."
			);
		}

		BigDecimal low = cheaper(diesel, gasoline);
		BigDecimal high = dearer(diesel, gasoline);
		BigDecimal factor = style == TravelStyle.ECONOMIC ? ECONOMIC_FACTOR : BigDecimal.ONE;
		BigDecimal litersMin = vacationProperties.getCarFuelRoundTripLitersMin().multiply(factor);
		BigDecimal litersMax = vacationProperties.getCarFuelRoundTripLitersMax().multiply(factor);
		if (litersMin.compareTo(litersMax) > 0) {
			BigDecimal swap = litersMin;
			litersMin = litersMax;
			litersMax = swap;
		}
		if (litersMax.signum() <= 0 || low.signum() <= 0) {
			return new ResolvedCarFuel(
					null,
					ResolvedCarFuel.Status.UNAVAILABLE,
					"Güncel yakıt fiyatı yok; araç bütçesi katalog tahmininden, tek araç olarak yazıldı."
			);
		}

		MoneyRange amount = MoneyRange.ofTry(litersMin.multiply(low), litersMax.multiply(high));
		String liters = litersMin.stripTrailingZeros().toPlainString()
				+ "–"
				+ litersMax.stripTrailingZeros().toPlainString();
		return new ResolvedCarFuel(
				amount,
				ResolvedCarFuel.Status.FRESH,
				"Araç yakıtı tek araç için " + liters + " litrelik gidiş-dönüş bandı × güncel benzin ve dizel fiyatıdır. İl mesafesi yoktur."
		);
	}

	private Optional<BigDecimal> freshPrice(FuelType type, Instant cutoff) {
		return fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(type)
				.filter(row -> row.getCheckedAt() != null && !row.getCheckedAt().isBefore(cutoff))
				.map(row -> row.getPricePerLiter())
				.filter(price -> price != null && price.signum() > 0);
	}

	private static BigDecimal cheaper(Optional<BigDecimal> diesel, Optional<BigDecimal> gasoline) {
		if (diesel.isEmpty()) {
			return gasoline.orElseThrow();
		}
		if (gasoline.isEmpty()) {
			return diesel.orElseThrow();
		}
		return diesel.get().min(gasoline.get());
	}

	private static BigDecimal dearer(Optional<BigDecimal> diesel, Optional<BigDecimal> gasoline) {
		if (diesel.isEmpty()) {
			return gasoline.orElseThrow();
		}
		if (gasoline.isEmpty()) {
			return diesel.orElseThrow();
		}
		return diesel.get().max(gasoline.get());
	}
}
