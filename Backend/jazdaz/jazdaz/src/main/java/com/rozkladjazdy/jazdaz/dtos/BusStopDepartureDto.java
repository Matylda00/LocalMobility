package com.rozkladjazdy.jazdaz.dtos;

public record BusStopDepartureDto(
        String departureTime,
        String arrivalTime,
        Integer departureSeconds,
        Integer stopSequence,
        Long tripId,
        String tripExternalId,
        String headsign
) {
}