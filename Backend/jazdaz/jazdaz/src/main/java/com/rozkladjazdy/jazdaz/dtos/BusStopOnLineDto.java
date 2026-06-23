package com.rozkladjazdy.jazdaz.dtos;

import java.math.BigDecimal;

public record BusStopOnLineDto(
        Long id,
        String externalId,
        String stopCode,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String platformCode,
        Integer stopSequence
) {
}