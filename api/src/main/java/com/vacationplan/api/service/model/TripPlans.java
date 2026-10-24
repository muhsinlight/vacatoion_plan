package com.vacationplan.api.service.model;

import java.util.List;
import java.util.Objects;

import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;

/**
 * HTTP gövdesinden plan üretir. Boş ulaşım, konaklama ve tarz burada varsayılır.
 */
public final class TripPlans {

	private TripPlans() {
	}

	public static TripPlan from(TripRequest request) {
		if (request.endDate().isBefore(request.startDate())) {
			throw new IllegalArgumentException("Dönüş tarihi gidişten önce olamaz");
		}
		int adults = request.adults() == null ? 1 : request.adults();
		List<Integer> childAges = request.childAges() == null ? List.of() : List.copyOf(request.childAges());
		TransportMode transport = request.transportMode() == null ? TransportMode.BUS : request.transportMode();
		AccommodationType lodging = request.accommodationType();
		if (lodging == null) {
			lodging = request.endDate().isEqual(request.startDate())
					? AccommodationType.NONE
					: AccommodationType.HOTEL;
		}
		TravelStyle style = request.travelStyle() == null ? TravelStyle.BALANCED : request.travelStyle();
		boolean includeContingency = !Boolean.FALSE.equals(request.includeContingency());
		List<String> selected = request.selectedActivityIds() == null
				? null
				: List.copyOf(request.selectedActivityIds());
		return new TripPlan(
				request.originCity(),
				request.destinationCity(),
				new TripStay(request.startDate(), request.endDate()),
				adults,
				childAges,
				adults + childAges.size(),
				transport,
				lodging,
				style,
				request.totalBudget(),
				request.perPersonBudget(),
				includeContingency,
				selected,
				customNames(request.customActivityNames())
		);
	}

	private static List<String> customNames(List<String> names) {
		if (names == null) {
			return List.of();
		}
		return names.stream()
				.filter(name -> name != null && !name.isBlank())
				.map(name -> {
					String trimmed = Objects.requireNonNull(name).trim();
					return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
				})
				.distinct()
				.limit(8)
				.toList();
	}
}
