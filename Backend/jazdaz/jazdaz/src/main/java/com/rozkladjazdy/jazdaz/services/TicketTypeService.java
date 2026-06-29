package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.database.entities.TicketType;
import com.rozkladjazdy.jazdaz.database.repositories.TicketTypeRepository;
import com.rozkladjazdy.jazdaz.dtos.TicketTypeDto;
import com.rozkladjazdy.jazdaz.dtos.TicketTypesResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;

    public TicketTypeService(TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public TicketTypesResponse getTicketTypes() {
        List<TicketTypeDto> ticketTypes = ticketTypeRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();

        return new TicketTypesResponse(ticketTypes);
    }

    private TicketTypeDto toDto(TicketType ticketType) {
        return new TicketTypeDto(
                ticketType.getName(),
                ticketType.getPrice(),
                ticketType.getDurationMinutes(),
                ticketType.getTicketCategory()
        );
    }
}