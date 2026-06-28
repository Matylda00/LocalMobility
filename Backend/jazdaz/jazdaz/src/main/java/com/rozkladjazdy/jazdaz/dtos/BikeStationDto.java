package com.rozkladjazdy.jazdaz.dtos;

public record BikeStationDto(
        String stationId,
        String name,
        Double latitude,
        Double longitude,
        Integer availableBikes
) {}