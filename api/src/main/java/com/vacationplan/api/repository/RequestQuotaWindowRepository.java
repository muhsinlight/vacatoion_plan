package com.vacationplan.api.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.RequestQuotaWindow;

public interface RequestQuotaWindowRepository extends JpaRepository<RequestQuotaWindow, Long> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(
			value = """
					INSERT INTO public.request_quota_windows (bucket, scope_key, window_start, used_count)
					VALUES (:bucket, :scopeKey, :windowStart, 1)
					ON CONFLICT (bucket, scope_key, window_start)
					DO UPDATE SET used_count = request_quota_windows.used_count + 1
					WHERE request_quota_windows.used_count < :max
					""",
			nativeQuery = true
	)
	int insertOrIncrement(
			@Param("bucket") String bucket,
			@Param("scopeKey") String scopeKey,
			@Param("windowStart") Instant windowStart,
			@Param("max") int max
	);
}
