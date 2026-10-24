package com.vacationplan.api.service;

import com.vacationplan.api.model.ResolvedCarFuel;
import com.vacationplan.api.model.TravelStyle;

/**
 * Araç bütçesi için güncel benzin ve dizel litre fiyatını okur.
 */
public interface CarFuelQuoteService {

	/**
	 * Tek araç, gidiş-dönüş litre bandı × taze yakıt fiyatı.
	 *
	 * @param style ekonomik tarz litre bandını 0,90 ile çarpar
	 * @return taze tutar veya fiyat yoksa {@code UNAVAILABLE}
	 */
	ResolvedCarFuel quote(TravelStyle style);
}
