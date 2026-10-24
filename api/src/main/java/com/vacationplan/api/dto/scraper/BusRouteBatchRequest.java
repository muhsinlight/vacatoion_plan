package com.vacationplan.api.dto.scraper;

import java.util.List;

public record BusRouteBatchRequest(List<BusScrapeLeg> legs) {
}
