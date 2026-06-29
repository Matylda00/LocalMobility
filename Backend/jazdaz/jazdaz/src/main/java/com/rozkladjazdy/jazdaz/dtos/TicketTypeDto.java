package com.rozkladjazdy.jazdaz.dtos;

import java.math.BigDecimal;

public record TicketTypeDto(
        String name,
        BigDecimal price,
        Integer durationMinutes,
        String ticketCategory
) {
}