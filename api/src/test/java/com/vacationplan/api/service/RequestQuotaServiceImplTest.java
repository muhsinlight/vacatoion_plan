package com.vacationplan.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.repository.RequestQuotaWindowRepository;
import com.vacationplan.api.service.impl.RequestQuotaServiceImpl;

@ExtendWith(MockitoExtension.class)
class RequestQuotaServiceImplTest {

	@Mock
	private RequestQuotaWindowRepository repository;

	private VacationProperties properties;
	private Clock clock;
	private RequestQuotaServiceImpl service;
	private Instant windowStart;

	@BeforeEach
	void setUp() {
		properties = new VacationProperties();
		clock = Clock.fixed(Instant.parse("2026-10-15T10:40:00Z"), ZoneOffset.UTC);
		windowStart = Instant.parse("2026-10-15T10:00:00Z");
		service = new RequestQuotaServiceImpl(repository, properties, clock);
	}

	@Test
	void allowsFirstRequestInHour() {
		when(repository.insertOrIncrement("SCRAPER", "shared", windowStart, 20)).thenReturn(1);

		assertTrue(service.tryAcquire(QuotaBucket.SCRAPER));
	}

	@Test
	void incrementsUnderTheCap() {
		when(repository.insertOrIncrement("SCRAPER", "shared", windowStart, 20)).thenReturn(1);

		assertTrue(service.tryAcquire(QuotaBucket.SCRAPER));
		verify(repository, never()).save(any());
	}

	@Test
	void deniesWhenHourlyMaxReached() {
		when(repository.insertOrIncrement("SCRAPER", "shared", windowStart, 20)).thenReturn(0);

		assertFalse(service.tryAcquire(QuotaBucket.SCRAPER));
	}

	@Test
	void unlimitedWhenMaxIsZero() {
		properties.getQuota().setScraperMaxPerHour(0);

		assertTrue(service.tryAcquire(QuotaBucket.SCRAPER));
		verify(repository, never()).insertOrIncrement(anyString(), anyString(), any(), anyInt());
	}

	@Test
	void windowIsTruncatedToHour() {
		when(repository.insertOrIncrement("GROQ", "shared", windowStart, 30)).thenReturn(1);

		assertTrue(service.tryAcquire(QuotaBucket.GROQ));
	}
}
