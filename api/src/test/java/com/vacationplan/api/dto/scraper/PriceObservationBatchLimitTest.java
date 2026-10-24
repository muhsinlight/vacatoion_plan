package com.vacationplan.api.dto.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.PriceUnit;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PriceObservationBatchLimitTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsMoreThanTwoHundredObservations() {
		assertThat(validator.validate(batch(PriceObservationBatchRequest.MAX_OBSERVATIONS))).isEmpty();
		assertThat(validator.validate(batch(PriceObservationBatchRequest.MAX_OBSERVATIONS + 1)))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("observations");
	}

	private static PriceObservationBatchRequest batch(int size) {
		List<PriceObservationDto> observations = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			observations.add(new PriceObservationDto(
					BudgetCategory.FOOD,
					PriceScopeType.DESTINATION,
					"izmir",
					PriceUnit.PER_DAY,
					new BigDecimal("100"),
					new BigDecimal("150"),
					new BigDecimal("200"),
					"TRY",
					PriceSourceType.SCRAPE,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null));
		}
		return new PriceObservationBatchRequest(observations);
	}
}
