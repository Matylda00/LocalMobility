package com.rozkladjazdy.jazdaz;

import com.rozkladjazdy.jazdaz.services.WarsawBusLocationService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
class BusLocationIntegrationTest {

    private static HttpServer fakeWarsawApiServer;

    private static final AtomicReference<String> apiResponseBody =
            new AtomicReference<>(twoBusesResponse());

    private static final AtomicInteger apiStatusCode =
            new AtomicInteger(200);

    private static final AtomicInteger apiRequestCount =
            new AtomicInteger(0);

    private static final AtomicReference<String> lastRequestMethod =
            new AtomicReference<>();

    private static final AtomicReference<String> lastAuthorizationHeader =
            new AtomicReference<>();

    private static final AtomicReference<String> lastRequestBody =
            new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarsawBusLocationService warsawBusLocationService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        startFakeWarsawApiServer();

        registry.add(
                "warsaw.bus-locations.url",
                () -> "http://localhost:" + fakeWarsawApiServer.getAddress().getPort()
                        + "/api/action/get_ztm_lokalizacja_pojazdow"
        );
        registry.add("warsaw.bus-locations.api-key", () -> "test-api-key");
        registry.add("warsaw.bus-locations.refresh-ms", () -> "3600000");
    }

    @BeforeEach
    void setUp() {
        apiResponseBody.set(twoBusesResponse());
        apiStatusCode.set(200);
        apiRequestCount.set(0);
        lastRequestMethod.set(null);
        lastAuthorizationHeader.set(null);
        lastRequestBody.set(null);

        warsawBusLocationService.refreshBusLocations();
    }

    @AfterAll
    static void stopFakeWarsawApiServer() {
        if (fakeWarsawApiServer != null) {
            fakeWarsawApiServer.stop(0);
        }
    }

    @Test
    void shouldReturnBusLocationsLoadedFromWarsawApi() throws Exception {
        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.buses", hasSize(2)))
                .andExpect(jsonPath("$.buses[0].line", is("175")))
                .andExpect(jsonPath("$.buses[0].latitude", is(52.2297)))
                .andExpect(jsonPath("$.buses[0].longitude", is(21.0122)))
                .andExpect(jsonPath("$.buses[1].line", is("128")))
                .andExpect(jsonPath("$.buses[1].latitude", is(52.24)))
                .andExpect(jsonPath("$.buses[1].longitude", is(21.01)))
                .andExpect(jsonPath("$.buses[0].brigade").doesNotExist())
                .andExpect(jsonPath("$.buses[0].time").doesNotExist())
                .andExpect(jsonPath("$.count").doesNotExist())
                .andExpect(jsonPath("$.fetchedAt").doesNotExist());
    }

    @Test
    void shouldCallWarsawApiUsingPostAuthorizationHeaderAndTypeBody() {
        assertEquals(1, apiRequestCount.get());
        assertEquals("POST", lastRequestMethod.get());
        assertEquals("test-api-key", lastAuthorizationHeader.get());

        String body = lastRequestBody.get();

        assertTrue(body.contains("\"type\""));
        assertTrue(body.contains("1"));
    }

    @Test
    void shouldReturnCachedLocationsWithoutCallingWarsawApiOnEndpointRequest() throws Exception {
        int requestsBeforeEndpointCall = apiRequestCount.get();

        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses", hasSize(2)));

        assertEquals(requestsBeforeEndpointCall, apiRequestCount.get());
    }

    @Test
    void shouldUpdateReturnedLocationsAfterRefresh() throws Exception {
        apiResponseBody.set("""
                [
                  {
                    "Brigade": "7",
                    "Lines": "189",
                    "Lat": 52.1111,
                    "Lon": 21.2222,
                    "Time": "2026-06-28 12:10:00",
                    "VehicleNumber": "9001"
                  }
                ]
                """);

        warsawBusLocationService.refreshBusLocations();

        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses", hasSize(1)))
                .andExpect(jsonPath("$.buses[0].line", is("189")))
                .andExpect(jsonPath("$.buses[0].latitude", is(52.1111)))
                .andExpect(jsonPath("$.buses[0].longitude", is(21.2222)));
    }

    @Test
    void shouldReturnEmptyListWhenWarsawApiReturnsEmptyArray() throws Exception {
        apiResponseBody.set("[]");

        warsawBusLocationService.refreshBusLocations();

        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses", hasSize(0)));
    }

    @Test
    void shouldSkipBusesWithoutRequiredLocationData() throws Exception {
        apiResponseBody.set("""
                [
                  {
                    "Brigade": "1",
                    "Lines": "175",
                    "Lat": 52.2297,
                    "Lon": 21.0122,
                    "Time": "2026-06-28 12:00:00",
                    "VehicleNumber": "1001"
                  },
                  {
                    "Brigade": "2",
                    "Lines": "128",
                    "Lat": null,
                    "Lon": 21.0100,
                    "Time": "2026-06-28 12:00:05",
                    "VehicleNumber": "1002"
                  },
                  {
                    "Brigade": "3",
                    "Lines": null,
                    "Lat": 52.2400,
                    "Lon": 21.0100,
                    "Time": "2026-06-28 12:00:05",
                    "VehicleNumber": "1003"
                  }
                ]
                """);

        warsawBusLocationService.refreshBusLocations();

        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses", hasSize(1)))
                .andExpect(jsonPath("$.buses[0].line", is("175")))
                .andExpect(jsonPath("$.buses[0].latitude", is(52.2297)))
                .andExpect(jsonPath("$.buses[0].longitude", is(21.0122)));
    }

    @Test
    void shouldKeepLastValidLocationsWhenWarsawApiFails() throws Exception {
        apiStatusCode.set(500);
        apiResponseBody.set("""
                {
                  "error": "temporary error"
                }
                """);

        warsawBusLocationService.refreshBusLocations();

        mockMvc.perform(get("/api/bus-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses", hasSize(2)))
                .andExpect(jsonPath("$.buses[0].line", is("175")))
                .andExpect(jsonPath("$.buses[1].line", is("128")));
    }

    private static void startFakeWarsawApiServer() {
        if (fakeWarsawApiServer != null) {
            return;
        }

        try {
            fakeWarsawApiServer = HttpServer.create(new InetSocketAddress(0), 0);

            fakeWarsawApiServer.createContext("/api/action/get_ztm_lokalizacja_pojazdow", exchange -> {
                apiRequestCount.incrementAndGet();
                lastRequestMethod.set(exchange.getRequestMethod());
                lastAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));

                String requestBody = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );
                lastRequestBody.set(requestBody);

                byte[] response = apiResponseBody.get().getBytes(StandardCharsets.UTF_8);

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

    private static String twoBusesResponse() {
        return """
                [
                  {
                    "Brigade": "4",
                    "Lines": "175",
                    "Lat": 52.2297,
                    "Lon": 21.0122,
                    "Time": "2026-06-28 12:00:00",
                    "VehicleNumber": "1001"
                  },
                  {
                    "Brigade": "2",
                    "Lines": "128",
                    "Lat": 52.2400,
                    "Lon": 21.0100,
                    "Time": "2026-06-28 12:00:05",
                    "VehicleNumber": "1002"
                  }
                ]
                """;
    }
}