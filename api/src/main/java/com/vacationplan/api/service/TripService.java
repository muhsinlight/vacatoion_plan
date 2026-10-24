package com.vacationplan.api.service;

import com.vacationplan.api.dto.PageResponse;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.dto.TripSummaryResponse;

/**
 * Kayıtlı seyahatlerin yaşam döngüsü.
 */
public interface TripService {

	/**
	 * Planı hesaplar ve yeni seyahat olarak kaydeder.
	 *
	 * @param request seyahat gövdesi
	 * @return kaydın özeti
	 */
	TripSummaryResponse create(TripRequest request);

	/**
	 * Kimliğe göre seyahat ve kayıtlı bütçe.
	 *
	 * @param id seyahat kimliği
	 * @return seyahat özeti
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	TripSummaryResponse getById(Long id);

	/**
	 * Oturumdaki kullanıcının seyahat özetleri (kayıtlı bütçe satırlarıyla).
	 *
	 * @param page 0 tabanlı sayfa
	 * @param size sayfa boyutu (en fazla {@link com.vacationplan.api.util.PageParams#MAX_SIZE})
	 */
	PageResponse<TripSummaryResponse> listMine(int page, int size);

	/**
	 * Sahipsiz satırlar dahil bütün seyahatler. Admin yolu çağırır.
	 */
	PageResponse<TripSummaryResponse> listAllForAdmin(int page, int size);

	/**
	 * Gövdeyi yeniden hesaplayıp mevcut kaydın üzerine yazar.
	 *
	 * @param id seyahat kimliği
	 * @param request yeni seyahat gövdesi
	 * @return güncel özet
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	TripSummaryResponse update(Long id, TripRequest request);

	/**
	 * Seyahati ve bütçe satırlarını siler.
	 *
	 * @param id seyahat kimliği
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	void delete(Long id);

	/**
	 * Kayıtlı seyahatin bütçesini güncel fiyatlarla yeniler.
	 *
	 * @param id seyahat kimliği
	 * @return güncel özet
	 * @throws com.vacationplan.api.exception.TripNotFoundException kimlik yoksa
	 */
	TripSummaryResponse refreshBudget(Long id);

	/**
	 * Kayıtlı seyahat gövdesiyle önizleme (program, varsayımlar, canlı kanıtlar).
	 *
	 * @param id seyahat kimliği
	 * @return önizleme yanıtı
	 */
	TripPreviewResponse previewSaved(Long id);

	/**
	 * Kayıtlı seyahatin planlama gövdesi (önizleme / güncelleme için).
	 */
	TripRequest getRequest(Long id);
}
