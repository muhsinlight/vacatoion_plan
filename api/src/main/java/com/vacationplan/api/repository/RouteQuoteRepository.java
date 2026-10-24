package com.vacationplan.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.TransportMode;

public interface RouteQuoteRepository extends JpaRepository<RouteQuote, Long> {

	@Query("""
			select q from RouteQuote q
			left join fetch q.observation
			where q.originKey = :originKey
			  and q.destKey = :destKey
			  and q.transportMode = :transportMode
			  and q.weekBucket = :weekBucket
			""")
	Optional<RouteQuote> findByOriginKeyAndDestKeyAndTransportModeAndWeekBucket(
			@Param("originKey") String originKey,
			@Param("destKey") String destKey,
			@Param("transportMode") TransportMode transportMode,
			@Param("weekBucket") LocalDate weekBucket
	);

	@Query("""
			select q from RouteQuote q
			left join fetch q.observation
			where q.transportMode = :transportMode
			  and (
			    (q.originKey = :originKey and q.destKey = :destKey and q.weekBucket = :outboundWeek)
			    or (q.originKey = :destKey and q.destKey = :originKey and q.weekBucket = :returnWeek)
			  )
			""")
	List<RouteQuote> findBusRoundTrip(
			@Param("originKey") String originKey,
			@Param("destKey") String destKey,
			@Param("transportMode") TransportMode transportMode,
			@Param("outboundWeek") LocalDate outboundWeek,
			@Param("returnWeek") LocalDate returnWeek
	);
}
