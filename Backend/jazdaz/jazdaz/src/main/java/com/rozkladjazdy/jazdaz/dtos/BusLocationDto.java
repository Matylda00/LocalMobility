package com.rozkladjazdy.jazdaz.dtos;

public record BusLocationDto(
        String line,
        Double latitude,
        Double longitude
) {}