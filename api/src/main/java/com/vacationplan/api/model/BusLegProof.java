package com.vacationplan.api.model;

import java.math.BigDecimal;

/** Gidiş veya dönüşteki en ucuz müsait sefer. {@code url} Obilet arama sayfasıdır. */
public record BusLegProof(
		String direction,
		BigDecimal minAmount,
		String currency,
		String partner,
		String url
) {
}
