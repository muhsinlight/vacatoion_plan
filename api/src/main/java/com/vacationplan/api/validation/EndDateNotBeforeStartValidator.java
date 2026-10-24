package com.vacationplan.api.validation;

import com.vacationplan.api.dto.TripRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EndDateNotBeforeStartValidator implements ConstraintValidator<EndDateNotBeforeStart, TripRequest> {

	@Override
	public boolean isValid(TripRequest value, ConstraintValidatorContext context) {
		if (value == null || value.startDate() == null || value.endDate() == null || !value.endDate().isBefore(value.startDate())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode("endDate")
				.addConstraintViolation();
		return false;
	}
}
