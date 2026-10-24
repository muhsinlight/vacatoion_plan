package com.vacationplan.api.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacationplan.api.dto.SuggestedActivityDto;
import com.vacationplan.api.exception.ActivitySuggestionException;

public final class ActivitySuggestionParser {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Set<String> ALLOWED = Set.of("name", "description");
	private static final int MAX_ITEMS = 6;

	private ActivitySuggestionParser() {
	}

	public static List<SuggestedActivityDto> parse(String content) {
		if (content == null || content.isBlank()) {
			throw ActivitySuggestionException.emptyResponse();
		}
		int start = content.indexOf('[');
		int end = content.lastIndexOf(']');
		if (start < 0 || end <= start) {
			throw ActivitySuggestionException.notJsonArray();
		}
		JsonNode array;
		try {
			array = MAPPER.readTree(content.substring(start, end + 1));
		} catch (Exception ex) {
			throw ActivitySuggestionException.unreadable();
		}
		if (!array.isArray()) {
			throw ActivitySuggestionException.notJsonArray();
		}
		List<SuggestedActivityDto> result = new ArrayList<>();
		for (JsonNode node : array) {
			if (!node.isObject()) {
				throw ActivitySuggestionException.itemNotObject();
			}
			Iterator<String> fields = node.fieldNames();
			while (fields.hasNext()) {
				String field = fields.next();
				if (!ALLOWED.contains(field)) {
					throw ActivitySuggestionException.unexpectedField();
				}
			}
			String name = text(node, "name");
			if (name == null || name.isBlank()) {
				continue;
			}
			String description = text(node, "description");
			result.add(new SuggestedActivityDto(name.trim(), description == null ? "" : description.trim()));
			if (result.size() == MAX_ITEMS) {
				break;
			}
		}
		return List.copyOf(result);
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asText();
	}
}
