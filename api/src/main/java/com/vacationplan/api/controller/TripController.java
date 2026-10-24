package com.vacationplan.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vacationplan.api.dto.PageResponse;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.util.PageParams;
import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.dto.TripSummaryResponse;
import com.vacationplan.api.service.TripPreviewService;
import com.vacationplan.api.service.TripService;

import jakarta.validation.Valid;

/**
 * Kayıtlı seyahat ve kayıtsız önizleme uçları. Taban yol {@code /api/v1/trips}.
 */
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

	private final TripPreviewService tripPreviewService;
	private final TripService tripService;

	public TripController(TripPreviewService tripPreviewService, TripService tripService) {
		this.tripPreviewService = tripPreviewService;
		this.tripService = tripService;
	}

	/**
	 * Bütçe ve program önizlemesi üretir; seyahati kaydetmez.
	 *
	 * @param request doğrulanmış seyahat gövdesi
	 * @return bütçe, program ve varsayımlar
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException varış kataloğunda yoksa
	 */
	@PostMapping("/preview")
	public TripPreviewResponse preview(@Valid @RequestBody TripRequest request) {
		return tripPreviewService.preview(request);
	}

	/**
	 * Önizlemeyi hesaplar ve seyahati kaydeder.
	 *
	 * @param request doğrulanmış seyahat gövdesi
	 * @return kaydın özeti
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException varış kataloğunda yoksa
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripSummaryResponse create(@Valid @RequestBody TripRequest request) {
		return tripService.create(request);
	}

	/**
	 * Kayıtlı seyahatlerin sayfalanmış özet listesi (kayıtlı bütçe satırlarıyla).
	 *
	 * @param page 0 tabanlı sayfa
	 * @param size sayfa boyutu (varsayılan 20, en fazla 100)
	 */
	@GetMapping
	public PageResponse<TripSummaryResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + PageParams.DEFAULT_SIZE) int size) {
		return tripService.listMine(page, size);
	}

	/**
	 * Tek seyahatin özeti ve kayıtlı bütçe satırları.
	 *
	 * @param id seyahat kimliği
	 * @return seyahat özeti
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	@GetMapping("/{id}")
	public TripSummaryResponse get(@PathVariable Long id) {
		return tripService.getById(id);
	}

	/**
	 * Kayıtlı seyahat için program ve bütçe önizlemesi (kaydetmez).
	 */
	@GetMapping("/{id}/preview")
	public TripPreviewResponse previewSaved(@PathVariable Long id) {
		return tripService.previewSaved(id);
	}

	/**
	 * Kayıtlı seyahatin planlama gövdesi.
	 */
	@GetMapping("/{id}/request")
	public TripRequest getRequest(@PathVariable Long id) {
		return tripService.getRequest(id);
	}

	/**
	 * Seyahati yeni gövdeyle yeniden hesaplayıp kaydeder.
	 *
	 * @param id seyahat kimliği
	 * @param request doğrulanmış seyahat gövdesi
	 * @return güncel özet
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException varış kataloğunda yoksa
	 */
	@PutMapping("/{id}")
	public TripSummaryResponse update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
		return tripService.update(id, request);
	}

	/**
	 * Kayıtlı seyahatin bütçesini güncel fiyatlarla yeniden yazar.
	 *
	 * @param id seyahat kimliği
	 * @return güncel özet
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException varış kataloğunda yoksa
	 */
	@PostMapping("/{id}/refresh-budget")
	public TripSummaryResponse refreshBudget(@PathVariable Long id) {
		return tripService.refreshBudget(id);
	}

	/**
	 * Seyahati ve bütçe satırlarını siler.
	 *
	 * @param id seyahat kimliği
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		tripService.delete(id);
	}
}
