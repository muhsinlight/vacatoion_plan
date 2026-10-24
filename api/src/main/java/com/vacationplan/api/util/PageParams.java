package com.vacationplan.api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageParams {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	private PageParams() {
	}

	public static Pageable tripList(int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.min(MAX_SIZE, Math.max(1, size));
		return PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
	}
}
