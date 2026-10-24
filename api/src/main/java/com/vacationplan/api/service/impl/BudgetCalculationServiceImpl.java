package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.service.BudgetCalculationService;
import com.vacationplan.api.service.model.TripPlan;

@Service
public class BudgetCalculationServiceImpl implements BudgetCalculationService {

	private static final BigDecimal CHILD_TRANSPORT_FACTOR = new BigDecimal("0.50");

	@Override
	public BudgetBreakdown calculate(TripPlan plan, List<BudgetLineItem> rawItems) {
		List<BudgetLineItem> itemsInput = Objects.requireNonNull(rawItems, "rawItems");
		List<BudgetLineItem> selected = itemsInput.stream()
				.filter(item -> item.selected())
				.toList();

		List<BudgetLineItem> priced = new ArrayList<>(selected);

		List<BudgetLineItem> stored = new ArrayList<>();
		for (BudgetLineItem item : itemsInput) {
			if (!item.selected()) {
				stored.add(item);
			}
		}
		stored.addAll(priced);

		Map<BudgetCategory, MoneyRange> byCategory = new EnumMap<>(BudgetCategory.class);
		for (BudgetCategory category : BudgetCategory.values()) {
			byCategory.put(category, MoneyRange.zeroTry());
		}
		for (BudgetLineItem item : priced) {
			byCategory.put(item.category(), byCategory.get(item.category()).plus(item.amount()));
		}

		MoneyRange total = sum(priced);
		int travelers = Math.max(plan.travelerCount(), 1);
		int days = Math.max(plan.dayCount(), 1);

		MoneyRange perPerson = total.multiply(BigDecimal.ONE.divide(BigDecimal.valueOf(travelers), 8, RoundingMode.HALF_UP));
		MoneyRange dailyAverage = total.multiply(BigDecimal.ONE.divide(BigDecimal.valueOf(days), 8, RoundingMode.HALF_UP));

		MoneyRange estimatedOnly = priced.stream()
				.filter(item -> item.sourceType() == PriceSourceType.ESTIMATE)
				.map(item -> item.amount())
				.reduce(MoneyRange.zeroTry(), (left, right) -> left.plus(right));

		MoneyRange known = priced.stream()
				.filter(item -> item.sourceType() != PriceSourceType.ESTIMATE)
				.map(item -> item.amount())
				.reduce(MoneyRange.zeroTry(), (left, right) -> left.plus(right));

		return new BudgetBreakdown(stored, byCategory, total, perPerson, dailyAverage, estimatedOnly, known);
	}

	@Override
	public BigDecimal adultEquivalent(TripPlan plan) {
		BigDecimal adults = BigDecimal.valueOf(plan.adults());
		BigDecimal children = CHILD_TRANSPORT_FACTOR.multiply(BigDecimal.valueOf(plan.childAges().size()));
		return adults.add(children);
	}

	private static MoneyRange sum(List<BudgetLineItem> items) {
		return items.stream()
				.map(item -> item.amount())
				.reduce(MoneyRange.zeroTry(), (left, right) -> left.plus(right));
	}
}
