package com.vacationplan.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.vacationplan.api.dto.scraper.BusRouteBatchRequest;
import com.vacationplan.api.dto.scraper.BusRouteBatchResponse;
import com.vacationplan.api.dto.scraper.LodgingSearchRequest;
import com.vacationplan.api.dto.scraper.LodgingSearchResponse;
import com.vacationplan.api.dto.scraper.PriceObservationBatchRequest;
import com.vacationplan.api.dto.scraper.PriceObservationBatchResponse;
import com.vacationplan.api.dto.scraper.ScrapeIngestRequest;
import com.vacationplan.api.dto.scraper.ScrapeTargetDto;
import com.vacationplan.api.util.CityKeyNormalizer;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Kök contract/ altındaki örnekler scraper/tests/test_contract.py ile aynı dosyalar. */
class ScraperContractTest {

	private static final JsonMapper MAPPER = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	static Stream<Arguments> bodies() {
		return Stream.of(
				Arguments.of("price_observation_batch.json", PriceObservationBatchRequest.class),
				Arguments.of("price_observation_batch_response.json", PriceObservationBatchResponse.class),
				Arguments.of("bus_route_batch_request.json", BusRouteBatchRequest.class),
				Arguments.of("bus_route_batch_response.json", BusRouteBatchResponse.class),
				Arguments.of("lodging_search_request.json", LodgingSearchRequest.class),
				Arguments.of("lodging_search_response.json", LodgingSearchResponse.class),
				Arguments.of("scrape_target.json", ScrapeTargetDto.class),
				Arguments.of("scrape_ingest_request.json", ScrapeIngestRequest.class)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void dtoRoundTripKeepsEveryField(String file, Class<?> type) throws IOException {
		JsonNode fixture = MAPPER.readTree(read(file));
		JsonNode roundTrip = MAPPER.valueToTree(MAPPER.treeToValue(fixture, type));
		assertThat(paths(roundTrip, "")).isEqualTo(paths(fixture, ""));
	}

	@Test
	void enumsMatchContract() throws Exception {
		JsonNode enums = MAPPER.readTree(read("enums.json"));
		for (Map.Entry<String, JsonNode> entry : enums.properties()) {
			Class<?> type = Class.forName("com.vacationplan.api.model." + entry.getKey());
			var expected = new TreeSet<String>();
			entry.getValue().forEach(value -> expected.add(value.asString()));
			var actual = new TreeSet<String>();
			Arrays.stream(type.getEnumConstants()).forEach(value -> actual.add(((Enum<?>) value).name()));
			assertThat(actual).as(entry.getKey()).isEqualTo(expected);
		}
	}

	@Test
	void provincesFileHasEightyOneAndMatchesClasspath() throws IOException {
		JsonNode contract = MAPPER.readTree(read("tr_provinces.json"));
		JsonNode classpath;
		try (var in = ScraperContractTest.class.getResourceAsStream("/tr_provinces.json")) {
			classpath = MAPPER.readTree(in);
		}
		assertThat(contract.get("provinces")).hasSize(81);
		assertThat(classpath).isEqualTo(contract);
	}

	@Test
	void cityKeysMatchContract() throws IOException {
		for (JsonNode row : MAPPER.readTree(read("city_keys.json"))) {
			assertThat(CityKeyNormalizer.normalize(row.get("input").asString()))
					.as(row.get("input").asString())
					.isEqualTo(row.get("key").asString());
		}
	}

	private static TreeSet<String> paths(JsonNode node, String prefix) {
		var result = new TreeSet<String>();
		if (node.isObject()) {
			for (Map.Entry<String, JsonNode> entry : node.properties()) {
				String path = prefix + "/" + entry.getKey();
				result.add(path);
				result.addAll(paths(entry.getValue(), path));
			}
		} else if (node.isArray()) {
			for (JsonNode item : node) {
				result.addAll(paths(item, prefix + "[]"));
			}
		}
		return result;
	}

	private static String read(String file) throws IOException {
		for (Path dir : new Path[] { Path.of("..", "contract"), Path.of("contract") }) {
			Path candidate = dir.resolve(file);
			if (Files.isRegularFile(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
		}
		throw new IOException("contract/" + file + " bulunamadı");
	}
}
