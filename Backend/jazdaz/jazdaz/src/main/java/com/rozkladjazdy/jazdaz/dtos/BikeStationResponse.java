package com.rozkladjazdy.jazdaz.dtos;

import java.util.List;

public record BikeStationResponse(
        List<BikeStationDto> stations
) {}