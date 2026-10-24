package com.vacationplan.api.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Açılış kataloğu. İller {@code tr_provinces.json} ile aynı anahtarları taşır.
 * Bantlar tek ulusal tabandır; il bazlı fiyat yoktur.
 */
public record CatalogSeed(Bands bands, List<Province> provinces) {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public record Bands(
			BigDecimal busRoundTripMin,
			BigDecimal busRoundTripMax,
			BigDecimal trainRoundTripMin,
			BigDecimal trainRoundTripMax,
			BigDecimal planeRoundTripMin,
			BigDecimal planeRoundTripMax,
			BigDecimal carRoundTripMin,
			BigDecimal carRoundTripMax,
			BigDecimal lodgingPerNightMin,
			BigDecimal lodgingPerNightMax,
			BigDecimal foodPerPersonPerDayMin,
			BigDecimal foodPerPersonPerDayMax,
			BigDecimal localTransportPerDayMin,
			BigDecimal localTransportPerDayMax
	) {
	}

	public record Province(String cityKey, String displayName) {
	}

	public static CatalogSeed load() {
		try (InputStream in = CatalogSeed.class.getResourceAsStream("/catalog-seed.json")) {
			return MAPPER.readValue(in, CatalogSeed.class);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
