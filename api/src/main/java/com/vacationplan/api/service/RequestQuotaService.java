package com.vacationplan.api.service;

import com.vacationplan.api.model.QuotaBucket;

/**
 * Saatlik istek kotası.
 */
public interface RequestQuotaService {

	/**
	 * Kovadan bir hak düşer.
	 *
	 * @param bucket kota kovası
	 * @return hak alındıysa {@code true}
	 */
	boolean tryAcquire(QuotaBucket bucket);
}
