package com.rozkladjazdy.jazdaz.dtos;

import com.rozkladjazdy.jazdaz.dtos.BusStopOnLineDto;

import java.util.List;

public record BusLineStopsResponse(
        String lineNumber,
        Integer directionId,
        String headsign,
        Long representativeTripId,
        String representativeTripExternalId,
        List<BusStopOnLineDto> stops
) {
}