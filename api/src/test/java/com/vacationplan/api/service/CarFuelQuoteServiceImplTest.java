package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.FuelPrice;
import com.vacationplan.api.model.FuelType;
import com.vacationplan.api.model.ResolvedCarFuel;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.FuelPriceRepository;
import com.vacationplan.api.service.impl.CarFuelQuoteServiceImpl;

@ExtendWith(MockitoExtension.class)
class CarFuelQuoteServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

	@Mock
	private FuelPriceRepository fuelPriceRepository;

	private CarFuelQuoteServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CarFuelQuoteServiceImpl(
				fuelPriceRepository,
				new VacationProperties(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void freshGasolineAndDieselSpanTheLiterBand() {
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.DIESEL))
				.thenReturn(Optional.of(fuel(FuelType.DIESEL, "50.00", NOW)));
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.GASOLINE))
				.thenReturn(Optional.of(fuel(FuelType.GASOLINE, "54.00", NOW)));

		ResolvedCarFuel quote = service.quote(TravelStyle.BALANCED);

		assertThat(quote.status()).isEqualTo(ResolvedCarFuel.Status.FRESH);
		assertThat(quote.amount().min()).isEqualByComparingTo("1250.00");
		assertThat(quote.amount().max()).isEqualByComparingTo("2970.00");
	}

	@Test
	void economicStyleShrinksLiters() {
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.DIESEL))
				.thenReturn(Optional.of(fuel(FuelType.DIESEL, "40.00", NOW)));
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.GASOLINE))
				.thenReturn(Optional.empty());

		ResolvedCarFuel quote = service.quote(TravelStyle.ECONOMIC);

		assertThat(quote.amount().min()).isEqualByComparingTo("900.00");
		assertThat(quote.amount().max()).isEqualByComparingTo("1980.00");
	}

	@Test
	void staleFuelFallsBack() {
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.DIESEL))
				.thenReturn(Optional.of(fuel(FuelType.DIESEL, "50.00", NOW.minus(java.time.Duration.ofDays(15)))));
		when(fuelPriceRepository.findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType.GASOLINE))
				.thenReturn(Optional.empty());

		ResolvedCarFuel quote = service.quote(TravelStyle.BALANCED);

		assertThat(quote.status()).isEqualTo(ResolvedCarFuel.Status.UNAVAILABLE);
		assertThat(quote.amount()).isNull();
	}

	private static FuelPrice fuel(FuelType type, String price, Instant checkedAt) {
		FuelPrice row = new FuelPrice();
		row.setFuelType(type);
		row.setPricePerLiter(new BigDecimal(price));
		row.setEffectiveFrom(LocalDate.of(2026, 10, 19));
		row.setCheckedAt(checkedAt);
		return row;
	}
}
