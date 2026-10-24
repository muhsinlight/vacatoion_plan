package com.vacationplan.api.service.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record TripStay(LocalDate start, LocalDate end) {

	public int nightCount() {
		return (int) Math.max(ChronoUnit.DAYS.between(start, end), 0);
	}

	public int dayCount() {
		return nightCount() + 1;
	}
}
