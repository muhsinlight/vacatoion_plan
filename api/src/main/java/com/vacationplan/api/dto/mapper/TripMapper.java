package com.vacationplan.api.dto.mapper;

import java.util.List;

import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.model.TripStatus;
import com.vacationplan.api.service.model.TripPlan;

public final class TripMapper {

	private TripMapper() {
	}

	public static Trip fromPlan(TripPlan plan, Destination destination) {
		Trip trip = new Trip();
		applyPlan(trip, plan, destination);
		trip.setStatus(TripStatus.PLANNED);
		return trip;
	}

	public static void applyPlan(Trip trip, TripPlan plan, Destination destination) {
		trip.setOriginCity(plan.originCity());
		trip.setDestinationCity(plan.destinationCity());
		trip.setDestination(destination);
		trip.setStartDate(plan.stay().start());
		trip.setEndDate(plan.stay().end());
		trip.setAdults(plan.adults());
		trip.setChildAges(plan.childAges());
		trip.setTransportMode(plan.transportMode());
		trip.setAccommodationType(plan.accommodationType());
		trip.setTravelStyle(plan.travelStyle());
		trip.setTotalBudget(plan.totalBudget());
		trip.setPerPersonBudget(plan.perPersonBudget());
		trip.setIncludeContingency(plan.includeContingency());
		trip.setSelectedActivityKeys(plan.selectedActivityIds());
		trip.setCustomActivityNames(plan.customActivityNames());
	}

	public static void copyNotes(Trip trip, TripRequest request) {
		trip.setInterests(request.interests() == null ? List.of() : request.interests());
		trip.setSpecialNeeds(request.specialNeeds());
	}

	public static TripRequest toRequest(Trip trip) {
		return new TripRequest(
				trip.getOriginCity(),
				trip.getDestinationCity(),
				trip.getStartDate(),
				trip.getEndDate(),
				trip.getAdults(),
				trip.getChildAges(),
				trip.getTransportMode(),
				trip.getAccommodationType(),
				trip.getTravelStyle(),
				trip.getInterests(),
				trip.getTotalBudget(),
				trip.getPerPersonBudget(),
				trip.isIncludeContingency(),
				trip.getSelectedActivityKeys(),
				trip.getSpecialNeeds(),
				trip.getCustomActivityNames()
		);
	}
}
