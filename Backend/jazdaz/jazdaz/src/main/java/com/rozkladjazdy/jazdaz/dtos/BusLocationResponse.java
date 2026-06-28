package com.rozkladjazdy.jazdaz.dtos;

import java.util.List;

public record BusLocationResponse(
        List<BusLocationDto> buses
) {}