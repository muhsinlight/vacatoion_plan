package com.vacationplan.api.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.repository.RequestQuotaWindowRepository;
import com.vacationplan.api.security.UserPrincipal;
import com.vacationplan.api.service.RequestQuotaService;

@Service
public class RequestQuotaServiceImpl implements RequestQuotaService {

	private static final String SHARED = "shared";

	private final RequestQuotaWindowRepository requestQuotaWindowRepository;
	private final VacationProperties vacationProperties;
	private final Clock clock;

	public RequestQuotaServiceImpl(
			RequestQuotaWindowRepository requestQuotaWindowRepository,
			VacationProperties vacationProperties,
			Clock clock
	) {
		this.requestQuotaWindowRepository = requestQuotaWindowRepository;
		this.vacationProperties = vacationProperties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public boolean tryAcquire(QuotaBucket bucket) {
		int max = maxFor(bucket);
		if (max <= 0) {
			return true;
		}
		Instant windowStart = clock.instant().truncatedTo(ChronoUnit.HOURS);
		return requestQuotaWindowRepository.insertOrIncrement(
				bucket.name(),
				currentScopeKey(),
				windowStart,
				max
		) == 1;
	}

	private static String currentScopeKey() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
			return "user:" + principal.getId();
		}
		return SHARED;
	}

	private int maxFor(QuotaBucket bucket) {
		VacationProperties.Quota quota = vacationProperties.getQuota();
		return switch (bucket) {
			case SCRAPER -> quota.getScraperMaxPerHour();
			case GROQ -> quota.getGroqMaxPerHour();
		};
	}
}
