package com.rozkladjazdy.jazdaz.dtos;

public record ParkingDto(
        String name,
        Double latitude,
        Double longitude,
        Integer availableSpaces
) {
}