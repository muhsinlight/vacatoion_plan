package com.vacationplan.api.service;

import com.vacationplan.api.dto.AuthUserResponse;

public interface AuthService {

	/**
	 * Yeni kullanıcıyı USER rolüyle kaydeder ve oturum açar.
	 *
	 * @param firstName ad
	 * @param lastName soyad
	 * @param email e-posta
	 * @param password düz şifre
	 * @return access, refresh ve kullanıcı özeti
	 * @throws com.vacationplan.api.exception.AuthFailureException e-posta kayıtlıysa
	 */
	AuthSession register(String firstName, String lastName, String email, String password);

	/**
	 * Oturumdaki kullanıcının adını ve soyadını günceller.
	 *
	 * @param userId kullanıcı kimliği
	 * @param firstName ad
	 * @param lastName soyad
	 * @return güncel kullanıcı özeti
	 */
	AuthUserResponse updateProfile(Long userId, String firstName, String lastName);

	/**
	 * E-posta ve parolayı doğrular. Başarılı giriş önceki oturumları kapatır.
	 *
	 * @param email e-posta
	 * @param password düz parola
	 * @return access, refresh ve kullanıcı özeti
	 * @throws com.vacationplan.api.exception.AuthFailureException parola, hesap veya e-posta uyuşmazsa
	 */
	AuthSession login(String email, String password);

	/**
	 * Refresh değerini bir kez iptal eder ve yeni çift üretir. Kısa pencerede aynı değer
	 * aynı yeni çifti döner. Pencere dışında tekrar tüm oturumları kapatır.
	 *
	 * @param rawRefresh çerezdeki refresh
	 * @return yeni access, refresh ve kullanıcı özeti
	 * @throws com.vacationplan.api.exception.AuthFailureException değer yok, süresi dolmuş veya iptalse
	 */
	AuthSession rotate(String rawRefresh);

	/**
	 * Tek refresh kaydını iptal eder. Yoksa bir şey yapmaz.
	 *
	 * @param rawRefresh çerezdeki refresh
	 */
	void revoke(String rawRefresh);

	/**
	 * Bu cihazın refresh ve access değerlerini iptal eder. Diğer cihazlar açık kalır.
	 *
	 * @param rawRefresh çerezdeki refresh; filtre döndürdüyse yeni değer
	 * @param presentedAccess istekteki access
	 * @param rotatedAccess filtre aynı istekte ürettiyse onun access değeri
	 */
	void logoutCurrent(String rawRefresh, String presentedAccess, String rotatedAccess);

	/**
	 * Kullanıcının açık refresh kayıtlarını iptal eder ve access sürümünü artırır.
	 *
	 * @param userId kullanıcı kimliği
	 */
	void revokeAll(Long userId);
}
