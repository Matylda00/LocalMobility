package com.rozkladjazdy.jazdaz;

import com.rozkladjazdy.jazdaz.services.WarsawParkingService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ParkingIntegrationTest {

    private static final int FAKE_WARSAW_API_PORT = 18081;

    private static HttpServer fakeWarsawApiServer;
    private static final AtomicReference apiResponseBody = new AtomicReference(twoParkingsResponse());
    private static final AtomicInteger apiStatusCode = new AtomicInteger(200);
    private static final AtomicInteger apiRequestCount = new AtomicInteger(0);
    private static final AtomicReference lastRequestMethod = new AtomicReference();
    private static final AtomicReference lastAuthorizationHeader = new AtomicReference();
    private static final AtomicReference lastAcceptHeader = new AtomicReference();

    static {
        startFakeWarsawApiServer();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarsawParkingService warsawParkingService;

    @BeforeEach
    void setUp() {
        apiResponseBody.set(twoParkingsResponse());
        apiStatusCode.set(200);
        apiRequestCount.set(0);
        lastRequestMethod.set(null);
        lastAuthorizationHeader.set(null);
        lastAcceptHeader.set(null);

        warsawParkingService.refreshParkings();
    }

    @AfterAll
    static void stopFakeWarsawApiServer() {
        if (fakeWarsawApiServer != null) {
            fakeWarsawApiServer.stop(0);
        }
    }

    @Test
    void shouldReturnParkingsLoadedFromWarsawApi() throws Exception {
        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parkings", hasSize(2)))

                .andExpect(jsonPath("$.parkings[0].name", is("Parking na Pl. Żelaznej Bramy")))
                .andExpect(jsonPath("$.parkings[0].latitude", closeTo(52.239560, 0.000001)))
                .andExpect(jsonPath("$.parkings[0].longitude", closeTo(21.001779, 0.000001)))
                .andExpect(jsonPath("$.parkings[0].availableSpaces", is(23)))

                .andExpect(jsonPath("$.parkings[1].name", is("Parking przy ul. Filtrowej")))
                .andExpect(jsonPath("$.parkings[1].latitude", closeTo(52.218590, 0.000001)))
                .andExpect(jsonPath("$.parkings[1].longitude", closeTo(21.008419, 0.000001)))
                .andExpect(jsonPath("$.parkings[1].availableSpaces", is(30)))

                .andExpect(jsonPath("$.parkings[0].adress").doesNotExist())
                .andExpect(jsonPath("$.parkings[0].opening_hours").doesNotExist())
                .andExpect(jsonPath("$.parkings[0].tariffs").doesNotExist())
                .andExpect(jsonPath("$.parkings[0].free_places_total").doesNotExist());
    }

    @Test
    void shouldCallWarsawApiUsingPostAuthorizationHeaderAndAcceptJson() {
        assertEquals(1, apiRequestCount.get());
        assertEquals("POST", lastRequestMethod.get());
        assertEquals("test-api-key", lastAuthorizationHeader.get());
        assertTrue(lastAcceptHeader.get().toString().contains("application/json"));
    }

    @Test
    void shouldReturnCachedParkingsWithoutCallingWarsawApiOnEndpointRequest() throws Exception {
        int requestsBeforeEndpointCall = apiRequestCount.get();

        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkings", hasSize(2)));

        assertEquals(requestsBeforeEndpointCall, apiRequestCount.get());
    }

    @Test
    void shouldUpdateReturnedParkingsAfterRefresh() throws Exception {
        apiResponseBody.set("""
                {
                  "Status": 1,
                  "Timestamp": "2026-06-28T18:18:05",
                  "carParks": [
                    {
                      "name": "Parking testowy",
                      "latitude": "52.111111",
                      "longitude": "21.222222",
                      "free_places_total": {
                        "public": 7,
                        "disabled": 1,
                        "electric": 2
                      }
                    }
                  ]
                }
                """);

        warsawParkingService.refreshParkings();

        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkings", hasSize(1)))
                .andExpect(jsonPath("$.parkings[0].name", is("Parking testowy")))
                .andExpect(jsonPath("$.parkings[0].latitude", closeTo(52.111111, 0.000001)))
                .andExpect(jsonPath("$.parkings[0].longitude", closeTo(21.222222, 0.000001)))
                .andExpect(jsonPath("$.parkings[0].availableSpaces", is(10)));
    }

    @Test
    void shouldReturnEmptyListWhenWarsawApiReturnsEmptyCarParks() throws Exception {
        apiResponseBody.set("""
                {
                  "Status": 1,
                  "Timestamp": "2026-06-28T18:18:05",
                  "carParks": []
                }
                """);

        warsawParkingService.refreshParkings();

        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkings", hasSize(0)));
    }

    @Test
    void shouldSkipParkingsWithoutRequiredLocationData() throws Exception {
        apiResponseBody.set("""
                {
                  "Status": 1,
                  "Timestamp": "2026-06-28T18:18:05",
                  "carParks": [
                    {
                      "name": "Parking poprawny",
                      "latitude": "52.239560",
                      "longitude": "21.001779",
                      "free_places_total": {
                        "public": 5,
                        "disabled": 0,
                        "electric": 0
                      }
                    },
                    {
                      "name": "Parking bez latitude",
                      "latitude": null,
                      "longitude": "21.001779",
                      "free_places_total": {
                        "public": 10,
                        "disabled": 0,
                        "electric": 0
                      }
                    },
                    {
                      "name": "Parking bez wolnych miejsc",
                      "latitude": "52.239560",
                      "longitude": "21.001779",
                      "free_places_total": null
                    }
                  ]
                }
                """);

        warsawParkingService.refreshParkings();

        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkings", hasSize(1)))
                .andExpect(jsonPath("$.parkings[0].name", is("Parking poprawny")))
                .andExpect(jsonPath("$.parkings[0].availableSpaces", is(5)));
    }

    @Test
    void shouldKeepLastValidParkingsWhenWarsawApiFails() throws Exception {
        apiStatusCode.set(500);
        apiResponseBody.set("""
                {
                  "error": "temporary error"
                }
                """);

        warsawParkingService.refreshParkings();

        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkings", hasSize(2)))
                .andExpect(jsonPath("$.parkings[0].name", is("Parking na Pl. Żelaznej Bramy")))
                .andExpect(jsonPath("$.parkings[1].name", is("Parking przy ul. Filtrowej")));
    }

    private static void startFakeWarsawApiServer() {
        if (fakeWarsawApiServer != null) {
            return;
        }

        try {
            fakeWarsawApiServer = HttpServer.create(new InetSocketAddress(FAKE_WARSAW_API_PORT), 0);
            fakeWarsawApiServer.createContext("/api/action/get_m_parkingi_wolne_miejsca", exchange -> {
                apiRequestCount.incrementAndGet();
                lastRequestMethod.set(exchange.getRequestMethod());
                lastAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                lastAcceptHeader.set(exchange.getRequestHeaders().getFirst("Accept"));

                byte[] response = apiResponseBody.get().toString().getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(apiStatusCode.get(), response.length);

                try (var responseBody = exchange.getResponseBody()) {
                    responseBody.write(response);
                }
            });
            fakeWarsawApiServer.start();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String twoParkingsResponse() {
        return """
                {
                  "Status": 1,
                  "Timestamp": "2026-06-28T18:18:05",
                  "carParks": [
                    {
                      "name": "Parking na Pl. Żelaznej Bramy",
                      "latitude": "52.239560",
                      "longitude": "21.001779",
                      "adress": "",
                      "opening_hours": [
                        {
                          "monday": "00:00-23:59",
                          "tuesday": "00:00-23:59"
                        }
                      ],
                      "tariffs": [],
                      "free_places_total": {
                        "public": 23,
                        "disabled": 0,
                        "electric": 0
                      }
                    },
                    {
                      "name": "Parking przy ul. Filtrowej",
                      "latitude": "52.218590",
                      "longitude": "21.008419",
                      "adress": "",
                      "opening_hours": [
                        {
                          "monday": "00:00-23:59",
                          "tuesday": "00:00-23:59"
                        }
                      ],
                      "tariffs": [],
                      "free_places_total": {
                        "public": 30,
                        "disabled": 0,
                        "electric": 0
                      }
                    }
                  ]
                }
                """;
    }
}