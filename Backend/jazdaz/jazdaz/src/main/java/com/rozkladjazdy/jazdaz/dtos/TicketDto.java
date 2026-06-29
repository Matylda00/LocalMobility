package com.rozkladjazdy.jazdaz.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketDto(
        String uuid,
        String name,
        BigDecimal price,
        Integer durationMinutes,
        String ticketCategory,
        String status,
        LocalDateTime purchasedAt,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        String qrCode
) {
}