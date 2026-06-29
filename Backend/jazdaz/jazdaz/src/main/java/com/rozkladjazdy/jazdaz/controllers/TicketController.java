package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.dtos.TicketTypesResponse;
import com.rozkladjazdy.jazdaz.services.TicketTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final TicketTypeService ticketTypeService;

    public TicketController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/api/ticket-types")
    public TicketTypesResponse getTicketTypes() {
        return ticketTypeService.getTicketTypes();
    }
}