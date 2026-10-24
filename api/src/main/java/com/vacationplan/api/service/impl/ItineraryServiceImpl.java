package com.vacationplan.api.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.vacationplan.api.dto.DayPlanDto;
import com.vacationplan.api.dto.DayPlanItemDto;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.service.ItineraryService;
import com.vacationplan.api.service.model.TripPlan;

@Service
public class ItineraryServiceImpl implements ItineraryService {

	@Override
	public List<DayPlanDto> build(TripPlan plan, List<ActivityOption> activities, List<String> selectedActivityIds) {
		Set<String> selected = new HashSet<>(selectedActivityIds);
		List<PlacedActivity> chosen = new ArrayList<>();
		for (ActivityOption activity : activities) {
			if (selected.contains(activity.id())) {
				chosen.add(new PlacedActivity(activity.name(), activity.estimatedMinutes(), activity.description()));
			}
		}
		for (String name : plan.customActivityNames()) {
			chosen.add(new PlacedActivity(name, 0, null));
		}

		int days = plan.dayCount();
		List<DayPlanDto> daysOut = new ArrayList<>();
		int activityIndex = 0;

		for (int day = 0; day < days; day++) {
			LocalDate date = plan.stay().start().plusDays(day);
			List<DayPlanItemDto> items = new ArrayList<>();
			items.add(new DayPlanItemDto("MEAL", "Kahvaltı", 60, null));
			if (activityIndex < chosen.size()) {
				PlacedActivity activity = chosen.get(activityIndex++);
				items.add(new DayPlanItemDto(
						"ACTIVITY",
						activity.name(),
						activity.estimatedMinutes(),
						activity.description()
				));
			}
			items.add(new DayPlanItemDto("MEAL", "Öğle yemeği", 90, null));
			if (day == 0) {
				items.add(new DayPlanItemDto("TRANSPORT", "Varış ve yerleşme", 120, null));
			}
			items.add(new DayPlanItemDto("MEAL", "Akşam yemeği", 90, null));
			daysOut.add(new DayPlanDto(day + 1, date, items));
		}

		while (activityIndex < chosen.size()) {
			DayPlanDto lastDay = daysOut.get(daysOut.size() - 1);
			PlacedActivity activity = chosen.get(activityIndex++);
			List<DayPlanItemDto> extended = new ArrayList<>(lastDay.items());
			extended.add(new DayPlanItemDto(
					"ACTIVITY",
					activity.name(),
					activity.estimatedMinutes(),
					activity.description()
			));
			daysOut.set(daysOut.size() - 1, new DayPlanDto(lastDay.dayNumber(), lastDay.date(), extended));
		}

		return daysOut;
	}

	private record PlacedActivity(String name, int estimatedMinutes, String description) {
	}
}
