package com.vacationplan.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Sayfalanmış liste yanıtı (0 tabanlı {@code page}).
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext,
		boolean hasPrevious
) {

	public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
		return new PageResponse<>(
				content,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext(),
				page.hasPrevious());
	}
}
