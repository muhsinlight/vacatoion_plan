package com.vacationplan.api.service.impl;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import com.vacationplan.api.dto.BudgetPreviewDto;
import com.vacationplan.api.dto.PageResponse;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.dto.TripSummaryResponse;
import com.vacationplan.api.dto.mapper.BudgetDtoMapper;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.entity.TripBudgetLine;
import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.exception.TripNotFoundException;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.TripBudgetLineRepository;
import com.vacationplan.api.repository.TripRepository;
import com.vacationplan.api.security.CurrentUser;
import com.vacationplan.api.service.PriceSnapshotService;
import com.vacationplan.api.service.TripPreviewService;
import com.vacationplan.api.service.TripService;
import com.vacationplan.api.dto.mapper.TripMapper;
import com.vacationplan.api.util.CityKeyNormalizer;
import com.vacationplan.api.util.PageParams;

@Service
public class TripServiceImpl implements TripService {

	private final TripRepository tripRepository;
	private final DestinationRepository destinationRepository;
	private final TripBudgetLineRepository tripBudgetLineRepository;
	private final TripPreviewService tripPreviewService;
	private final PriceSnapshotService priceSnapshotService;
	private final CurrentUser currentUser;
	private final Clock clock;
	private final TransactionOperations transactions;

	public TripServiceImpl(
			TripRepository tripRepository,
			DestinationRepository destinationRepository,
			TripBudgetLineRepository tripBudgetLineRepository,
			TripPreviewService tripPreviewService,
			PriceSnapshotService priceSnapshotService,
			CurrentUser currentUser,
			Clock clock,
			TransactionOperations transactions
	) {
		this.tripRepository = tripRepository;
		this.destinationRepository = destinationRepository;
		this.tripBudgetLineRepository = tripBudgetLineRepository;
		this.tripPreviewService = tripPreviewService;
		this.priceSnapshotService = priceSnapshotService;
		this.currentUser = currentUser;
		this.clock = clock;
		this.transactions = transactions;
	}

	// plan() scraper'a HTTP ile gider (en fazla vacation.scraper-read-timeout); transaction dışında kalmalı.
	@Override
	public TripSummaryResponse create(TripRequest request) {
		var plan = tripPreviewService.plan(request);
		return transactions.execute(status -> saveNewTrip(request, plan));
	}

	@Override
	@Transactional(readOnly = true)
	public TripSummaryResponse getById(Long id) {
		Trip trip = loadOwned(id);
		BudgetPreviewDto budget = loadBudgetFromDb(trip);
		return toSummary(trip, budget);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TripSummaryResponse> listMine(int page, int size) {
		Long ownerId = currentUser.requireEntity().getId();
		var tripPage = tripRepository.findByOwnerIdOrderByIdAsc(ownerId, PageParams.tripList(page, size));
		List<Trip> trips = tripPage.getContent();
		Map<Long, List<TripBudgetLine>> linesByTrip = loadBudgetLines(trips);
		var content = trips.stream()
				.map(trip -> toSummary(trip, BudgetDtoMapper.fromStoredLines(
						trip,
						linesByTrip.getOrDefault(trip.getId(), List.of()))))
				.toList();
		return PageResponse.of(content, tripPage);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TripSummaryResponse> listAllForAdmin(int page, int size) {
		var tripPage = tripRepository.findAllByOrderByIdAsc(PageParams.tripList(page, size));
		var content = tripPage.getContent().stream()
				.map(this::summaryWithoutBudget)
				.toList();
		return PageResponse.of(content, tripPage);
	}

	@Override
	public TripSummaryResponse update(Long id, TripRequest request) {
		transactions.executeWithoutResult(status -> loadOwned(id));
		var plan = tripPreviewService.plan(request);
		return transactions.execute(status -> {
			Trip trip = loadOwned(id);
			Destination destination = resolveDestination(plan.plan().destinationCity());
			TripMapper.applyPlan(trip, plan.plan(), destination);
			TripMapper.copyNotes(trip, request);
			trip.markUpdated(clock.instant());
			tripRepository.save(trip);
			replaceBudgetLines(trip, plan.budget());
			priceSnapshotService.saveForTrip(trip, plan);
			return toSummary(trip, BudgetDtoMapper.fromBreakdown(plan.budget()));
		});
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Trip trip = loadOwned(id);
		tripBudgetLineRepository.deleteByTripId(trip.getId());
		tripRepository.delete(trip);
	}

	@Override
	public TripPreviewResponse previewSaved(Long id) {
		TripRequest request = transactions.execute(status -> TripMapper.toRequest(loadOwned(id)));
		return tripPreviewService.preview(request);
	}

	@Override
	@Transactional(readOnly = true)
	public TripRequest getRequest(Long id) {
		return TripMapper.toRequest(loadOwned(id));
	}

	@Override
	public TripSummaryResponse refreshBudget(Long id) {
		var request = transactions.execute(status -> TripMapper.toRequest(loadOwned(id)));
		var plan = tripPreviewService.plan(request);
		return transactions.execute(status -> {
			Trip trip = loadOwned(id);
			replaceBudgetLines(trip, plan.budget());
			trip.markUpdated(clock.instant());
			tripRepository.save(trip);
			priceSnapshotService.saveForTrip(trip, plan);
			return toSummary(trip, BudgetDtoMapper.fromBreakdown(plan.budget()));
		});
	}

	private TripSummaryResponse saveNewTrip(TripRequest request, com.vacationplan.api.service.model.TripPlanResult plan) {
		Destination destination = resolveDestination(plan.plan().destinationCity());
		Trip trip = TripMapper.fromPlan(plan.plan(), destination);
		TripMapper.copyNotes(trip, request);
		trip.setOwner(currentUser.requireEntity());
		trip = tripRepository.save(trip);
		saveBudgetLines(trip, plan.budget());
		BudgetPreviewDto budget = BudgetDtoMapper.fromBreakdown(plan.budget());
		priceSnapshotService.saveForTrip(trip, plan);
		return toSummary(trip, budget);
	}

	private Trip loadOwned(Long id) {
		Trip trip = tripRepository.findById(id).orElseThrow(() -> new TripNotFoundException(id));
		User owner = currentUser.requireEntity();
		if (trip.getOwner() == null || !owner.getId().equals(trip.getOwner().getId())) {
			throw new TripNotFoundException(id);
		}
		return trip;
	}

	private TripSummaryResponse summaryWithoutBudget(Trip trip) {
		return new TripSummaryResponse(
				trip.getId(),
				trip.getStatus(),
				trip.getOriginCity(),
				trip.getDestinationCity(),
				trip.getStartDate(),
				trip.getEndDate(),
				null
		);
	}

	private Destination resolveDestination(String destinationCity) {
		return destinationRepository
				.findByCityKey(CityKeyNormalizer.normalize(destinationCity))
				.orElse(null);
	}

	private void replaceBudgetLines(Trip trip, BudgetBreakdown budget) {
		tripBudgetLineRepository.deleteByTripId(trip.getId());
		saveBudgetLines(trip, budget);
	}

	private void saveBudgetLines(Trip trip, BudgetBreakdown budget) {
		for (BudgetLineItem line : budget.lineItems()) {
			TripBudgetLine entity = new TripBudgetLine();
			entity.setTrip(trip);
			entity.setCategory(line.category());
			entity.setLabel(line.label());
			entity.setMinAmount(line.amount().min());
			entity.setMaxAmount(line.amount().max());
			entity.setCurrency(line.amount().currency());
			entity.setSourceType(line.sourceType());
			entity.setIncludedInTotal(line.selected());
			tripBudgetLineRepository.save(entity);
		}
	}

	private Map<Long, List<TripBudgetLine>> loadBudgetLines(List<Trip> trips) {
		if (trips.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = trips.stream().map(trip -> trip != null ? trip.getId() : null).toList();
		return tripBudgetLineRepository.findByTrip_IdInOrderByIdAsc(ids).stream()
				.collect(Collectors.groupingBy(line -> line.getTrip().getId()));
	}

	private BudgetPreviewDto loadBudgetFromDb(Trip trip) {
		List<TripBudgetLine> lines = tripBudgetLineRepository.findByTripIdOrderByIdAsc(trip.getId());
		return BudgetDtoMapper.fromStoredLines(trip, lines);
	}

	private TripSummaryResponse toSummary(Trip trip, BudgetPreviewDto budget) {
		return new TripSummaryResponse(
				trip.getId(),
				trip.getStatus(),
				trip.getOriginCity(),
				trip.getDestinationCity(),
				trip.getStartDate(),
				trip.getEndDate(),
				budget
		);
	}
}
