package com.vacationplan.api.dto;

import java.math.BigDecimal;

import com.vacationplan.api.model.BusLegProof;

public record BusLegDto(
		String direction,
		BigDecimal minAmount,
		String currency,
		String partner,
		String url
) {
	public static BusLegDto from(BusLegProof proof) {
		return new BusLegDto(
				proof.direction(),
				proof.minAmount(),
				proof.currency(),
				proof.partner(),
				proof.url()
		);
	}
}
