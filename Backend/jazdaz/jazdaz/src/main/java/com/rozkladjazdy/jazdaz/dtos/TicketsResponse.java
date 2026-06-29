package com.rozkladjazdy.jazdaz.dtos;

import java.util.List;

public record TicketsResponse(
        List<TicketDto> tickets
) {
}