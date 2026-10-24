package com.vacationplan.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.dto.TripRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EndDateNotBeforeStartValidatorTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void sameDayAndLaterReturnAreValid() {
		assertThat(validator.validate(request(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10)))).isEmpty();
		assertThat(validator.validate(request(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12)))).isEmpty();
	}

	@Test
	void returnBeforeDeparturePointsAtEndDate() {
		Set<ConstraintViolation<TripRequest>> violations = validator.validate(
				request(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 9)));

		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("endDate");
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Dönüş tarihi gidişten önce olamaz");
	}

	@Test
	void missingDatesStayWithNotNullInsteadOfDateOrder() {
		Set<ConstraintViolation<TripRequest>> violations = validator.validate(request(null, null));

		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("startDate", "endDate");
	}

	private static TripRequest request(LocalDate start, LocalDate end) {
		return new TripRequest(
				"Ankara",
				"İzmir",
				start,
				end,
				2,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}
}
