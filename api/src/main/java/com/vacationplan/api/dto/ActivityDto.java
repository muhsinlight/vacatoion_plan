package com.vacationplan.api.dto;

import com.vacationplan.api.model.ActivityOption;

public record ActivityDto(
		String id,
		String name,
		int estimatedMinutes,
		MoneyDto entryFeePerPerson,
		boolean defaultSelected
) {
	public static ActivityDto from(ActivityOption option) {
		return new ActivityDto(
				option.id(),
				option.name(),
				option.estimatedMinutes(),
				MoneyDto.from(option.entryFeePerPerson()),
				option.defaultSelected()
		);
	}
}
