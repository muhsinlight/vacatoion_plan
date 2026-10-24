package com.vacationplan.api.dto;

import java.util.List;

public record ActivitySuggestionResponse(List<SuggestedActivityDto> activities) {
}
