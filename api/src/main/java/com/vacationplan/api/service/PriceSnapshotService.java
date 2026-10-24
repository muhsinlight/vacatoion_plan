package com.vacationplan.api.service;

import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.service.model.TripPlanResult;

/**
 * İstenirse bütçe kalemlerinin anlık görüntüsünü {@code price_records} tablosuna yazar.
 */
public interface PriceSnapshotService {

	/**
	 * Plan kalemlerini seyahate bağlı anlık görüntü olarak kaydeder.
	 *
	 * @param trip kayıtlı seyahat
	 * @param plan hesaplanmış plan
	 */
	void saveForTrip(Trip trip, TripPlanResult plan);
}
