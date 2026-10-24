package com.vacationplan.api.dto.scraper;

import com.vacationplan.api.entity.ScrapeTarget;
import com.vacationplan.api.model.ScrapeCatalogField;
import com.vacationplan.api.model.ScrapeParserType;

public record ScrapeTargetDto(
		long id,
		String cityKey,
		ScrapeCatalogField catalogField,
		String activityKey,
		String sourceUrl,
		ScrapeParserType parserType,
		String label
) {
	public static ScrapeTargetDto from(ScrapeTarget target) {
		return new ScrapeTargetDto(
				target.getId(),
				target.getCityKey(),
				target.getCatalogField(),
				target.getActivityKey(),
				target.getSourceUrl(),
				target.getParserType(),
				target.getLabel());
	}
}
