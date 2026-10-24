package com.vacationplan.api.service;

import java.math.BigDecimal;
import java.util.List;

import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.service.model.TripPlan;

/**
 * Ham bütçe kalemlerini yolcu sayısına göre toplar.
 */
public interface BudgetCalculationService {

	/**
	 * Seçili kalemleri toplar. Seçilmemiş satır dökümde durur, toplama girmez.
	 *
	 * @param plan seyahat planı
	 * @param rawItems kalemler
	 * @return bütçe dökümü
	 */
	BudgetBreakdown calculate(TripPlan plan, List<BudgetLineItem> rawItems);

	/**
	 * Yetişkin eşdeğeri yolcu sayısı. Çocuk katsayısı 0,50'dir.
	 *
	 * @param plan seyahat planı
	 * @return eşdeğer yetişkin
	 */
	BigDecimal adultEquivalent(TripPlan plan);
}
