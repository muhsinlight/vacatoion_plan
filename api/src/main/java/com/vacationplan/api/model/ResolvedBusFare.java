package com.vacationplan.api.model;

import java.util.List;

/** {@code reason}: fiyat UNAVAILABLE veya STALE ise neden yenilenemediği; aksi halde null. */
public record ResolvedBusFare(MoneyRange amount, Status status, String reason, List<BusLegProof> legs) {

	public enum Status {
		FRESH,
		STALE,
		UNAVAILABLE
	}
}
