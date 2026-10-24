package com.vacationplan.api.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.bootstrap.CatalogSeed.Bands;
import com.vacationplan.api.bootstrap.CatalogSeed.Province;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.repository.DestinationRepository;

/**
 * Açılışta katalogda olmayan illeri ekler. Var olan satırın fiyatını değiştirmez.
 * {@code scrape_targets} bu koşuda yazılmaz.
 */
@Component
public class CatalogSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CatalogSeedRunner.class);

	private final DestinationRepository destinationRepository;

	public CatalogSeedRunner(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		CatalogSeed seed = CatalogSeed.load();
		Bands bands = seed.bands();
		int inserted = 0;
		int filled = 0;
		for (Province province : seed.provinces()) {
			var existing = destinationRepository.findByCityKey(province.cityKey());
			if (existing.isPresent()) {
				if (fillMissingModeBands(existing.get(), bands)) {
					destinationRepository.save(existing.get());
					filled++;
				}
				continue;
			}
			destinationRepository.save(newDestination(province, bands));
			inserted++;
		}
		log.info("Katalog tohumu: {} il eklendi, {} ilde eksik ulaşım bandı doldu", inserted, filled);
	}

	private static Destination newDestination(Province province, Bands bands) {
		Destination destination = new Destination();
		destination.setCityKey(province.cityKey());
		destination.setDisplayName(province.displayName());
		destination.setBusRoundTripMin(bands.busRoundTripMin());
		destination.setBusRoundTripMax(bands.busRoundTripMax());
		applyModeBands(destination, bands);
		destination.setLodgingPerNightMin(bands.lodgingPerNightMin());
		destination.setLodgingPerNightMax(bands.lodgingPerNightMax());
		destination.setFoodPerPersonPerDayMin(bands.foodPerPersonPerDayMin());
		destination.setFoodPerPersonPerDayMax(bands.foodPerPersonPerDayMax());
		destination.setLocalTransportPerDayMin(bands.localTransportPerDayMin());
		destination.setLocalTransportPerDayMax(bands.localTransportPerDayMax());
		return destination;
	}

	/** Var olan otobüs, konaklama ve yemek fiyatına dokunmaz. Yalnız boş mod bandını yazar. */
	private static boolean fillMissingModeBands(Destination destination, Bands bands) {
		boolean changed = false;
		if (destination.getTrainRoundTripMin() == null || destination.getTrainRoundTripMax() == null) {
			destination.setTrainRoundTripMin(bands.trainRoundTripMin());
			destination.setTrainRoundTripMax(bands.trainRoundTripMax());
			changed = true;
		}
		if (destination.getPlaneRoundTripMin() == null || destination.getPlaneRoundTripMax() == null) {
			destination.setPlaneRoundTripMin(bands.planeRoundTripMin());
			destination.setPlaneRoundTripMax(bands.planeRoundTripMax());
			changed = true;
		}
		if (destination.getCarRoundTripMin() == null || destination.getCarRoundTripMax() == null) {
			destination.setCarRoundTripMin(bands.carRoundTripMin());
			destination.setCarRoundTripMax(bands.carRoundTripMax());
			changed = true;
		}
		return changed;
	}

	private static void applyModeBands(Destination destination, Bands bands) {
		destination.setTrainRoundTripMin(bands.trainRoundTripMin());
		destination.setTrainRoundTripMax(bands.trainRoundTripMax());
		destination.setPlaneRoundTripMin(bands.planeRoundTripMin());
		destination.setPlaneRoundTripMax(bands.planeRoundTripMax());
		destination.setCarRoundTripMin(bands.carRoundTripMin());
		destination.setCarRoundTripMax(bands.carRoundTripMax());
	}
}
