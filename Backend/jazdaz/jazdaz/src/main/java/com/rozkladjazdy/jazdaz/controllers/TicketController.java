package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.dtos.PurchaseTicketRequest;
import com.rozkladjazdy.jazdaz.dtos.TicketDto;
import com.rozkladjazdy.jazdaz.dtos.TicketTypesResponse;
import com.rozkladjazdy.jazdaz.dtos.TicketsResponse;
import com.rozkladjazdy.jazdaz.services.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/api/ticket-types")
    public TicketTypesResponse getTicketTypes() {
        return ticketService.getTicketTypes();
    }

    @GetMapping("/api/tickets")
    public TicketsResponse getTickets(@AuthenticationPrincipal UserDetails userDetails) {
        return ticketService.getTicketsForUser(userDetails.getUsername());
    }

    @PostMapping("/api/tickets/{ticketUuid}/activate")
    public TicketDto activateTicket(
            @PathVariable String ticketUuid
    ) {
        return ticketService.activateTicket(ticketUuid);
    }
    @PostMapping("/api/tickets/purchase")
    public TicketDto purchaseTicket(
            @RequestBody PurchaseTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.purchaseTicket(request, userDetails.getUsername());
    }
}