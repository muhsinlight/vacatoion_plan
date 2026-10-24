package com.vacationplan.api.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.repository.DestinationRepository;

@ExtendWith(MockitoExtension.class)
class CatalogSeedRunnerTest {

	@Mock
	private DestinationRepository destinationRepository;

	@Test
	void seedMatchesProvinceList() throws Exception {
		CatalogSeed seed = CatalogSeed.load();
		Set<String> seedKeys = seed.provinces().stream()
				.map(province -> province.cityKey())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> provinces = new LinkedHashSet<>();
		try (InputStream in = CatalogSeed.class.getResourceAsStream("/tr_provinces.json")) {
			JsonNode root = new ObjectMapper().readTree(in);
			root.get("provinces").forEach(node -> provinces.add(node.asText()));
		}

		assertEquals(provinces, seedKeys);
		assertEquals("Isparta", seed.provinces().stream()
				.filter(province -> province.cityKey().equals("isparta"))
				.findFirst()
				.orElseThrow()
				.displayName());
	}

	@Test
	void insertsMissingProvinceAndLeavesExistingPrice() {
		Destination existing = new Destination();
		existing.setCityKey("isparta");
		existing.setDisplayName("Eski ad");
		existing.setBusRoundTripMin(new BigDecimal("1.00"));
		existing.setBusRoundTripMax(new BigDecimal("2.00"));
		existing.setTrainRoundTripMin(new BigDecimal("3.00"));
		existing.setTrainRoundTripMax(new BigDecimal("4.00"));
		existing.setPlaneRoundTripMin(new BigDecimal("5.00"));
		existing.setPlaneRoundTripMax(new BigDecimal("6.00"));
		existing.setCarRoundTripMin(new BigDecimal("7.00"));
		existing.setCarRoundTripMax(new BigDecimal("8.00"));

		when(destinationRepository.findByCityKey("isparta")).thenReturn(Optional.of(existing));
		when(destinationRepository.findByCityKey(argThat(key -> key != null && !key.equals("isparta"))))
				.thenReturn(Optional.empty());

		new CatalogSeedRunner(destinationRepository).run(null);

		verify(destinationRepository, never()).save(existing);
		assertEquals(new BigDecimal("1.00"), existing.getBusRoundTripMin());
		assertEquals(new BigDecimal("7.00"), existing.getCarRoundTripMin());
		assertEquals("Eski ad", existing.getDisplayName());

		ArgumentCaptor<Destination> saved = ArgumentCaptor.forClass(Destination.class);
		verify(destinationRepository, times(80)).save(saved.capture());
		Destination adana = saved.getAllValues().stream()
				.filter(destination -> destination.getCityKey().equals("adana"))
				.findFirst()
				.orElseThrow();
		assertEquals("Adana", adana.getDisplayName());
		assertEquals(new BigDecimal("900.00"), adana.getBusRoundTripMin());
		assertEquals(new BigDecimal("2200.00"), adana.getBusRoundTripMax());
		assertEquals(new BigDecimal("1500.00"), adana.getLodgingPerNightMin());
		assertEquals(new BigDecimal("400.00"), adana.getLocalTransportPerDayMax());
		assertEquals(new BigDecimal("1035.00"), adana.getCarRoundTripMin());
		assertEquals(new BigDecimal("5280.00"), adana.getPlaneRoundTripMax());
	}

	@Test
	void fillsMissingModeBandsWithoutChangingBusPrice() {
		Destination existing = new Destination();
		existing.setCityKey("isparta");
		existing.setBusRoundTripMin(new BigDecimal("1.00"));
		existing.setBusRoundTripMax(new BigDecimal("2.00"));

		when(destinationRepository.findByCityKey("isparta")).thenReturn(Optional.of(existing));
		when(destinationRepository.findByCityKey(argThat(key -> key != null && !key.equals("isparta"))))
				.thenReturn(Optional.empty());

		new CatalogSeedRunner(destinationRepository).run(null);

		assertEquals(new BigDecimal("1.00"), existing.getBusRoundTripMin());
		assertEquals(new BigDecimal("810.00"), existing.getTrainRoundTripMin());
		assertEquals(new BigDecimal("1035.00"), existing.getCarRoundTripMin());
		verify(destinationRepository).save(existing);
	}
}
