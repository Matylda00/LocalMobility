package com.rozkladjazdy.jazdaz;

import com.rozkladjazdy.jazdaz.database.entities.Ticket;
import com.rozkladjazdy.jazdaz.database.entities.TicketType;
import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.TicketRepository;
import com.rozkladjazdy.jazdaz.database.repositories.TicketTypeRepository;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import com.rozkladjazdy.jazdaz.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TicketIntegrationTest {

    private static final String USER_EMAIL = "user@test.com";
    private static final String OTHER_USER_EMAIL = "other@test.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity user;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new UserEntity(
                passwordEncoder.encode(PASSWORD),
                USER_EMAIL,
                UserRole.USER
        ));

        otherUser = userRepository.save(new UserEntity(
                passwordEncoder.encode(PASSWORD),
                OTHER_USER_EMAIL,
                UserRole.USER
        ));

        ticketTypeRepository.save(ticketType(
                "20-minutowy normalny",
                "3.40",
                20,
                "NORMAL"
        ));

        ticketTypeRepository.save(ticketType(
                "30-dniowy normalny",
                "110.00",
                43200,
                "NORMAL"
        ));
    }

    @Test
    void shouldReturnAvailableTicketTypesWithoutInternalIds() throws Exception {
        mockMvc.perform(get("/api/ticket-types")
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketTypes", hasSize(2)))
                .andExpect(jsonPath("$.ticketTypes[0].name", is("20-minutowy normalny")))
                .andExpect(jsonPath("$.ticketTypes[0].price", is(3.40)))
                .andExpect(jsonPath("$.ticketTypes[0].durationMinutes", is(20)))
                .andExpect(jsonPath("$.ticketTypes[0].ticketCategory", is("NORMAL")))
                .andExpect(jsonPath("$.ticketTypes[0].id").doesNotExist())
                .andExpect(jsonPath("$.ticketTypes[0].currency").doesNotExist())
                .andExpect(jsonPath("$.ticketTypes[1].name", is("30-dniowy normalny")))
                .andExpect(jsonPath("$.ticketTypes[1].price", is(110.00)))
                .andExpect(jsonPath("$.ticketTypes[1].durationMinutes", is(43200)))
                .andExpect(jsonPath("$.ticketTypes[1].ticketCategory", is("NORMAL")))
                .andExpect(jsonPath("$.ticketTypes[1].id").doesNotExist())
                .andExpect(jsonPath("$.ticketTypes[1].currency").doesNotExist());
    }

    @Test
    void shouldReturnEmptyTicketsForUserWithoutBoughtTickets() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tickets", hasSize(0)));
    }

    @Test
    void shouldPurchaseTicketUsingFakeCardData() throws Exception {
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic(USER_EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPurchaseRequest("20-minutowy normalny", "NORMAL")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.name", is("20-minutowy normalny")))
                .andExpect(jsonPath("$.price", is(3.40)))
                .andExpect(jsonPath("$.durationMinutes", is(20)))
                .andExpect(jsonPath("$.ticketCategory", is("NORMAL")))
                .andExpect(jsonPath("$.status", is("inactive")))
                .andExpect(jsonPath("$.purchasedAt").exists())
                .andExpect(jsonPath("$.validFrom").doesNotExist())
                .andExpect(jsonPath("$.validTo").doesNotExist())
                .andExpect(jsonPath("$.qrCode", startsWith("LOCALMOBILITY:TICKET:")))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.ticketTypeId").doesNotExist())
                .andExpect(jsonPath("$.currency").doesNotExist());

        assertEquals(1, ticketRepository.findByUser_EmailOrderByPurchaseDateDesc(USER_EMAIL).size());
    }

    @Test
    void shouldRejectPurchaseWhenTicketNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic(USER_EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketName": "",
                                  "ticketCategory": "NORMAL",
                                  "cardNumber": "4242424242424242",
                                  "expiryDate": "12/30",
                                  "cvv": "123",
                                  "cardHolder": "Jan Kowalski"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertTrue(ticketRepository.findAll().isEmpty());
    }

    @Test
    void shouldRejectPurchaseWhenCardDataIsInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic(USER_EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketName": "20-minutowy normalny",
                                  "ticketCategory": "NORMAL",
                                  "cardNumber": "abcd",
                                  "expiryDate": "12/30",
                                  "cvv": "123",
                                  "cardHolder": "Jan Kowalski"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertTrue(ticketRepository.findAll().isEmpty());
    }

    @Test
    void shouldRejectPurchaseWhenTicketTypeDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic(USER_EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPurchaseRequest("Nieistniejący bilet", "NORMAL")))
                .andExpect(status().isNotFound());

        assertTrue(ticketRepository.findAll().isEmpty());
    }

    @Test
    void shouldReturnOnlyTicketsBoughtByLoggedUser() throws Exception {
        buyTicket(USER_EMAIL, "20-minutowy normalny", "NORMAL");
        buyTicket(OTHER_USER_EMAIL, "30-dniowy normalny", "NORMAL");

        mockMvc.perform(get("/api/tickets")
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].name", is("20-minutowy normalny")))
                .andExpect(jsonPath("$.tickets[0].ticketCategory", is("NORMAL")))
                .andExpect(jsonPath("$.tickets[0].status", is("inactive")))
                .andExpect(jsonPath("$.tickets[0].uuid").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].qrCode", startsWith("LOCALMOBILITY:TICKET:")))
                .andExpect(jsonPath("$.tickets[0].id").doesNotExist())
                .andExpect(jsonPath("$.tickets[0].userId").doesNotExist());
    }

    @Test
    void shouldActivateInactiveTicketByUuidAndSetValidityDates() throws Exception {
        String ticketUuid = buyTicket(USER_EMAIL, "20-minutowy normalny", "NORMAL");

        mockMvc.perform(post("/api/tickets/{ticketUuid}/activate", ticketUuid)
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", is(ticketUuid)))
                .andExpect(jsonPath("$.name", is("20-minutowy normalny")))
                .andExpect(jsonPath("$.durationMinutes", is(20)))
                .andExpect(jsonPath("$.ticketCategory", is("NORMAL")))
                .andExpect(jsonPath("$.status", is("active")))
                .andExpect(jsonPath("$.validFrom").exists())
                .andExpect(jsonPath("$.validTo").exists())
                .andExpect(jsonPath("$.qrCode", startsWith("LOCALMOBILITY:TICKET:")));

        Ticket savedTicket = ticketRepository.findByUuid(ticketUuid).orElseThrow();
        assertEquals("active", savedTicket.getStatus());
        assertTrue(savedTicket.getValidFrom() != null);
        assertTrue(savedTicket.getValidTo() != null);
        assertEquals(20, java.time.Duration.between(
                savedTicket.getValidFrom(),
                savedTicket.getValidTo()
        ).toMinutes());
    }

    @Test
    void shouldReturnNotFoundWhenActivatingUnknownTicketUuid() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketUuid}/activate", UUID.randomUUID().toString())
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldExpireActiveTicketWhenGettingUserTicketsAndValidToIsInPast() throws Exception {
        String ticketUuid = buyTicket(USER_EMAIL, "20-minutowy normalny", "NORMAL");

        Ticket ticket = ticketRepository.findByUuid(ticketUuid).orElseThrow();
        ticket.setStatus("active");
        ticket.setValidFrom(LocalDateTime.now().minusMinutes(30));
        ticket.setValidTo(LocalDateTime.now().minusMinutes(10));
        ticketRepository.save(ticket);

        mockMvc.perform(get("/api/tickets")
                        .with(httpBasic(USER_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].uuid", is(ticketUuid)))
                .andExpect(jsonPath("$.tickets[0].status", is("expired")))
                .andExpect(jsonPath("$.tickets[0].validFrom").exists())
                .andExpect(jsonPath("$.tickets[0].validTo").exists());

        Ticket expiredTicket = ticketRepository.findByUuid(ticketUuid).orElseThrow();
        assertEquals("expired", expiredTicket.getStatus());
    }

    private String buyTicket(String email, String ticketName, String ticketCategory) throws Exception {
        String responseBody = mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic(email, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPurchaseRequest(ticketName, ticketCategory)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return responseBody.replaceAll(".*\"uuid\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private TicketType ticketType(
            String name,
            String price,
            int durationMinutes,
            String ticketCategory
    ) {
        TicketType ticketType = new TicketType();
        ticketType.setName(name);
        ticketType.setPrice(new BigDecimal(price));
        ticketType.setDurationMinutes(durationMinutes);
        ticketType.setTicketCategory(ticketCategory);
        return ticketType;
    }

    private String validPurchaseRequest(String ticketName, String ticketCategory) {
        return """
                {
                  "ticketName": "%s",
                  "ticketCategory": "%s",
                  "cardNumber": "4242 4242 4242 4242",
                  "expiryDate": "12/30",
                  "cvv": "123",
                  "cardHolder": "Jan Kowalski"
                }
                """.formatted(ticketName, ticketCategory);
    }
}