package com.rozkladjazdy.jazdaz.dtos;

import java.util.List;

public record ParkingResponse(
        List<ParkingDto> parkings
) {
}