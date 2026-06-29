package com.rozkladjazdy.jazdaz.dtos;

import java.util.List;

public record TicketTypesResponse(
        List<TicketTypeDto> ticketTypes
) {
}