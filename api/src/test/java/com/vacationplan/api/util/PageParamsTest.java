package com.vacationplan.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageParamsTest {

	@Test
	void clampsPageAndSize() {
		Pageable page = PageParams.tripList(-4, 0);

		assertThat(page.getPageNumber()).isZero();
		assertThat(page.getPageSize()).isEqualTo(1);
		assertThat(page.getSort()).isEqualTo(Sort.by("id").ascending());
	}

	@Test
	void capsSizeAtOneHundred() {
		Pageable page = PageParams.tripList(2, 500);

		assertThat(page.getPageNumber()).isEqualTo(2);
		assertThat(page.getPageSize()).isEqualTo(PageParams.MAX_SIZE);
	}
}
