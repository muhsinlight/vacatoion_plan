package com.vacationplan.api.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacationplan.api.dto.scraper.FoodPlaceDto;

public final class FoodPlacesJson {

	public static final int LIMIT = 6;

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TypeReference<List<FoodPlaceDto>> TYPE = new TypeReference<>() {
	};

	private FoodPlacesJson() {
	}

	public static String write(List<FoodPlaceDto> places) {
		try {
			return MAPPER.writeValueAsString(limit(places));
		} catch (JsonProcessingException ex) {
			return "[]";
		}
	}

	public static List<FoodPlaceDto> read(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return limit(MAPPER.readValue(json, TYPE));
		} catch (JsonProcessingException ex) {
			return List.of();
		}
	}

	private static List<FoodPlaceDto> limit(List<FoodPlaceDto> places) {
		if (places == null || places.isEmpty()) {
			return List.of();
		}
		List<FoodPlaceDto> kept = new ArrayList<>();
		for (FoodPlaceDto place : places) {
			if (place == null || place.name() == null || place.name().isBlank()) {
				continue;
			}
			kept.add(place);
			if (kept.size() == LIMIT) {
				break;
			}
		}
		return List.copyOf(kept);
	}
}
