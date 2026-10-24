package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.dto.ActivityDto;
import com.vacationplan.api.dto.DestinationDto;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.catalog.DestinationProfile;

/**
 * Destinasyon kataloğu ve katalog fiyat tahminleri.
 */
public interface CatalogService {

	/**
	 * Katalogdaki destinasyonlar.
	 *
	 * @return il anahtarı ve görünen ad
	 */
	List<DestinationDto> listDestinations();

	/**
	 * Tek destinasyon.
	 *
	 * @param cityKey il anahtarı
	 * @return destinasyon
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException anahtar yoksa
	 */
	DestinationDto getDestination(String cityKey);

	/**
	 * Destinasyonun katalog etkinlikleri.
	 *
	 * @param cityKey il anahtarı
	 * @return etkinlikler
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException anahtar yoksa
	 */
	List<ActivityDto> listActivities(String cityKey);

	/**
	 * Seyahat hesabı için destinasyon profili.
	 *
	 * @param destinationCity varış şehri
	 * @return katalog profili
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException şehir yoksa
	 */
	DestinationProfile resolveForTrip(String destinationCity);

	/**
	 * Ulaşım biçimine göre kişi başı gidiş-dönüş tahmin aralığı.
	 *
	 * @param profile destinasyon profili
	 * @param mode ulaşım biçimi
	 * @param style seyahat tarzı
	 * @return para aralığı
	 */
	MoneyRange transportQuote(DestinationProfile profile, TransportMode mode, TravelStyle style);

	/**
	 * Konaklama türüne göre gecelik tahmin aralığı.
	 *
	 * @param profile destinasyon profili
	 * @param type konaklama türü
	 * @return para aralığı
	 */
	MoneyRange lodgingQuote(DestinationProfile profile, AccommodationType type);
}
