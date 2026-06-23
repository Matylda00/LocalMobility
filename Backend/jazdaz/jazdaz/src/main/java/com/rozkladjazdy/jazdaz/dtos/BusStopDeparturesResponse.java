package com.rozkladjazdy.jazdaz.dtos;

import java.time.LocalDate;
import java.util.List;

public record BusStopDeparturesResponse(
        String lineNumber,
        Integer directionId,
        LocalDate date,
        Long stopId,
        String stopExternalId,
        String stopCode,
        String stopName,
        String platformCode,
        List<BusStopDepartureDto> departures
) {
}