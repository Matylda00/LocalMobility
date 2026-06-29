package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.database.entities.Ticket;
import com.rozkladjazdy.jazdaz.database.entities.TicketType;
import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.TicketRepository;
import com.rozkladjazdy.jazdaz.database.repositories.TicketTypeRepository;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import com.rozkladjazdy.jazdaz.dtos.*;
import com.rozkladjazdy.jazdaz.exceptions.BadDataException;
import com.rozkladjazdy.jazdaz.exceptions.ResourceExpiredException;
import com.rozkladjazdy.jazdaz.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketService {


    private static final String STATUS_INACTIVE = "inactive";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_EXPIRED = "expired";
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(
            TicketTypeRepository ticketTypeRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository
    ) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }
    @Transactional
    public TicketDto purchaseTicket(PurchaseTicketRequest request, String userEmail) {
        validatePurchaseRequest(request);

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(ResourceNotFoundException::new);

        TicketType ticketType = ticketTypeRepository.findByNameAndTicketCategory(
                        request.ticketName(),
                        request.ticketCategory()
                )
                .orElseThrow(ResourceNotFoundException::new);

        Ticket ticket = new Ticket();
        ticket.setUuid(UUID.randomUUID().toString());
        ticket.setUser(user);
        ticket.setTicketType(ticketType);
        ticket.setQrCode(generateUniqueQrCode());
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setValidFrom(null);
        ticket.setValidTo(null);
        ticket.setStatus("inactive");

        Ticket savedTicket = ticketRepository.save(ticket);

        return toTicketDto(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketTypesResponse getTicketTypes() {
        List<TicketTypeDto> ticketTypes = ticketTypeRepository.findAll()
                .stream()
                .map(this::toTicketTypeDto)
                .toList();

        return new TicketTypesResponse(ticketTypes);
    }

    @Transactional
    public TicketsResponse getTicketsForUser(String email) {
        List<Ticket> tickets = ticketRepository.findByUser_EmailOrderByPurchaseDateDesc(email);

        tickets.forEach(this::expireIfNeeded);

        List<TicketDto> ticketDtos = tickets.stream()
                .map(this::toTicketDto)
                .toList();

        return new TicketsResponse(ticketDtos);
    }
    private TicketTypeDto toTicketTypeDto(TicketType ticketType) {
        return new TicketTypeDto(
                ticketType.getName(),
                ticketType.getPrice(),
                ticketType.getDurationMinutes(),
                ticketType.getTicketCategory()
        );
    }

    private void validatePurchaseRequest(PurchaseTicketRequest request) {
        if (isBlank(request.ticketName())) {
            throw new BadDataException();
        }

        if (isBlank(request.ticketCategory())) {
            throw new BadDataException();
        }

        if (isBlank(request.cardHolder())) {
            throw new BadDataException();
        }

        String cardNumber = normalizeCardNumber(request.cardNumber());

        if (!cardNumber.matches("\\d{12,19}")) {
            throw new BadDataException();
        }

        if (isBlank(request.cvv()) || !request.cvv().matches("\\d{3,4}")) {
            throw new BadDataException();
        }

        validateExpiryDate(request.expiryDate());
    }

    private TicketDto toTicketDto(Ticket ticket) {
        TicketType ticketType = ticket.getTicketType();

        return new TicketDto(
                ticket.getUuid(),
                ticketType.getName(),
                ticketType.getPrice(),
                ticketType.getDurationMinutes(),
                ticketType.getTicketCategory(),
                normalizeStatus(ticket.getStatus()),
                ticket.getPurchaseDate(),
                ticket.getValidFrom(),
                ticket.getValidTo(),
                ticket.getQrCode()
        );
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }

        return status.toLowerCase(Locale.ROOT);
    }

    private void expireIfNeeded(Ticket ticket) {
        if (!STATUS_ACTIVE.equals(ticket.getStatus())) {
            return;
        }

        if (ticket.getValidTo() == null) {
            return;
        }

        if (ticket.getValidTo().isBefore(LocalDateTime.now())) {
            ticket.setStatus(STATUS_EXPIRED);
        }
    }
    @Transactional
    public TicketDto activateTicket(String ticketUuid) {
        Ticket ticket = ticketRepository.findByUuid(ticketUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        expireIfNeeded(ticket);

        if (!STATUS_INACTIVE.equals(ticket.getStatus())) {
            throw new ResourceExpiredException();
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(STATUS_ACTIVE);
        ticket.setValidFrom(now);
        ticket.setValidTo(now.plusMinutes(ticket.getTicketType().getDurationMinutes()));

        return toTicketDto(ticket);
    }
    private void validateExpiryDate(String expiryDate) {
        if (isBlank(expiryDate) || !expiryDate.matches("(0[1-9]|1[0-2])/(\\d{2}|\\d{4})")) {
            throw new BadDataException();
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        if (year < 100) {
            year += 2000;
        }

        YearMonth cardExpiry = YearMonth.of(year, month);
        YearMonth now = YearMonth.now();

        if (cardExpiry.isBefore(now)) {
            throw new ResourceExpiredException();
        }
    }
    private String normalizeCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return "";
        }

        return cardNumber.replaceAll("[\\s-]", "");
    }

    private String generateUniqueQrCode() {
        String qrCode;
        qrCode = "LOCALMOBILITY:TICKET:" + UUID.randomUUID();
        return qrCode;
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}