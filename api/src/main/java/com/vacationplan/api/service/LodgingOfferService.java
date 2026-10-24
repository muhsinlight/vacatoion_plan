package com.vacationplan.api.service;

import java.time.LocalDate;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TravelStyle;

/**
 * Konaklama teklifini önbellek veya scraper ile çözer.
 */
public interface LodgingOfferService {

	/**
	 * Tarih aralığı için gecelik konaklama.
	 *
	 * @param destinationCity varış
	 * @param checkIn giriş
	 * @param checkOut çıkış
	 * @param adults yetişkin sayısı
	 * @param accommodationType konaklama türü
	 * @param travelStyle seyahat tarzı
	 * @return çözülmüş konaklama
	 */
	ResolvedLodging resolve(
			String destinationCity,
			LocalDate checkIn,
			LocalDate checkOut,
			int adults,
			AccommodationType accommodationType,
			TravelStyle travelStyle
	);
}
