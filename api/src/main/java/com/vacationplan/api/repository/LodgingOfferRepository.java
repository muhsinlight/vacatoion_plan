package com.vacationplan.api.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.LodgingOffer;
import com.vacationplan.api.model.AccommodationType;

/** Sorgular açık yazılı: türetilmiş adda "CheckIn" içindeki "In" IN operatörü sayılıyor. */
public interface LodgingOfferRepository extends JpaRepository<LodgingOffer, Long> {

	@Query("""
			select o from LodgingOffer o
			where o.cityKey = :cityKey
			  and o.accommodationType = :accommodationType
			  and o.checkIn = :checkIn
			  and o.checkOut = :checkOut
			""")
	List<LodgingOffer> findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
			@Param("cityKey") String cityKey,
			@Param("accommodationType") AccommodationType accommodationType,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut
	);

	@Modifying(clearAutomatically = true)
	@Query("""
			delete from LodgingOffer o
			where o.cityKey = :cityKey
			  and o.accommodationType = :accommodationType
			  and o.checkIn = :checkIn
			  and o.checkOut = :checkOut
			""")
	void deleteByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
			@Param("cityKey") String cityKey,
			@Param("accommodationType") AccommodationType accommodationType,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut
	);
}
