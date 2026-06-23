package com.rozkladjazdy.jazdaz.dtos;


public record BusLineDto(
        Long id,
        String externalId,
        String lineNumber,
        String name
) {
}