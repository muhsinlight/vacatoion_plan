package com.vacationplan.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vacationplan.api.model.ResolvedBusFare;

/**
 * Gidiş-dönüş otobüs ücretini kayıtlı teklif veya scraper üzerinden çözer.
 */
public interface BusRouteFareService {

	/**
	 * İki bacaklı otobüs ücret aralığı.
	 *
	 * @param originCity kalkış
	 * @param destinationCity varış
	 * @param departure gidiş tarihi
	 * @param returning dönüş tarihi
	 * @param passengers yolcu sayısı
	 * @return çözülmüş ücret ve durumu
	 */
	ResolvedBusFare roundTrip(
			String originCity,
			String destinationCity,
			LocalDate departure,
			LocalDate returning,
			BigDecimal passengers
	);
}
