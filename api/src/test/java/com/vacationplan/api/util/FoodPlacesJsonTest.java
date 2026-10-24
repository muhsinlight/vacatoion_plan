package com.vacationplan.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.dto.scraper.FoodPlaceDto;

class FoodPlacesJsonTest {

	@Test
	void dropsBlankNamesAndKeepsSix() {
		List<FoodPlaceDto> places = new ArrayList<>();
		places.add(null);
		places.add(place("  "));
		for (int i = 1; i <= 7; i++) {
			places.add(place("Yer " + i));
		}

		List<FoodPlaceDto> read = FoodPlacesJson.read(FoodPlacesJson.write(places));

		assertThat(read).extracting(place -> place.name())
				.containsExactly("Yer 1", "Yer 2", "Yer 3", "Yer 4", "Yer 5", "Yer 6");
	}

	@Test
	void blankOrBrokenJsonIsEmpty() {
		assertThat(FoodPlacesJson.read(null)).isEmpty();
		assertThat(FoodPlacesJson.read("  ")).isEmpty();
		assertThat(FoodPlacesJson.read("{")).isEmpty();
		assertThat(FoodPlacesJson.write(null)).isEqualTo("[]");
	}

	private static FoodPlaceDto place(String name) {
		return new FoodPlaceDto(name, null, null, null, null, null, null);
	}
}
