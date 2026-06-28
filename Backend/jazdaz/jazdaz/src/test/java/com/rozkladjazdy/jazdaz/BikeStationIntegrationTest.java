package com.rozkladjazdy.jazdaz;

import com.rozkladjazdy.jazdaz.services.VeturiloBikeStationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class BikeStationIntegrationTest {

    private static final int FAKE_VETURILO_API_PORT = 18082;

    private static HttpServer fakeVeturiloApiServer;

    private static final AtomicReference<String> stationInformationResponseBody =
            new AtomicReference<>(twoStationsInformationResponse());

    private static final AtomicReference<String> stationStatusResponseBody =
            new AtomicReference<>(twoStationsStatusResponse());

    private static final AtomicInteger stationInformationStatusCode = new AtomicInteger(200);
    private static final AtomicInteger stationStatusStatusCode = new AtomicInteger(200);

    private static final AtomicInteger stationInformationRequestCount = new AtomicInteger(0);
    private static final AtomicInteger stationStatusRequestCount = new AtomicInteger(0);

    private static final AtomicReference<String> lastStationInformationRequestMethod =
            new AtomicReference<>();

    private static final AtomicReference<String> lastStationStatusRequestMethod =
            new AtomicReference<>();

    static {
        startFakeVeturiloApiServer();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VeturiloBikeStationService veturiloBikeStationService;

    @BeforeEach
    void setUp() {
        stationInformationResponseBody.set(twoStationsInformationResponse());
        stationStatusResponseBody.set(twoStationsStatusResponse());

        stationInformationStatusCode.set(200);
        stationStatusStatusCode.set(200);

        stationInformationRequestCount.set(0);
        stationStatusRequestCount.set(0);

        lastStationInformationRequestMethod.set(null);
        lastStationStatusRequestMethod.set(null);

        veturiloBikeStationService.refreshBikeStations();
    }

    @AfterAll
    static void stopFakeVeturiloApiServer() {
        if (fakeVeturiloApiServer != null) {
            fakeVeturiloApiServer.stop(0);
        }
    }

    @Test
    void shouldReturnBikeStationsLoadedFromVeturiloApi() throws Exception {
        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stations", hasSize(2)))
                .andExpect(jsonPath("$.stations[0].stationId", is("veturilo-1")))
                .andExpect(jsonPath("$.stations[0].name", is("Metro Centrum")))
                .andExpect(jsonPath("$.stations[0].latitude", closeTo(52.231111, 0.000001)))
                .andExpect(jsonPath("$.stations[0].longitude", closeTo(21.011111, 0.000001)))
                .andExpect(jsonPath("$.stations[0].availableBikes", is(7)))
                .andExpect(jsonPath("$.stations[1].stationId", is("veturilo-2")))
                .andExpect(jsonPath("$.stations[1].name", is("Rondo ONZ")))
                .andExpect(jsonPath("$.stations[1].latitude", closeTo(52.233333, 0.000001)))
                .andExpect(jsonPath("$.stations[1].longitude", closeTo(20.999999, 0.000001)))
                .andExpect(jsonPath("$.stations[1].availableBikes", is(3)))
                .andExpect(jsonPath("$.stations[0].capacity").doesNotExist())
                .andExpect(jsonPath("$.stations[0].availableDocks").doesNotExist())
                .andExpect(jsonPath("$.stations[0].isRenting").doesNotExist())
                .andExpect(jsonPath("$.stations[0].isReturning").doesNotExist())
                .andExpect(jsonPath("$.stations[0].lastReported").doesNotExist())
                .andExpect(jsonPath("$.stations[0].vehicleTypes").doesNotExist());
    }

    @Test
    void shouldCallVeturiloApiUsingGetRequests() {
        assertEquals(1, stationInformationRequestCount.get());
        assertEquals(1, stationStatusRequestCount.get());

        assertEquals("GET", lastStationInformationRequestMethod.get());
        assertEquals("GET", lastStationStatusRequestMethod.get());
    }

    @Test
    void shouldReturnCachedBikeStationsWithoutCallingVeturiloApiOnEndpointRequest() throws Exception {
        int stationInformationRequestsBeforeEndpointCall = stationInformationRequestCount.get();
        int stationStatusRequestsBeforeEndpointCall = stationStatusRequestCount.get();

        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(2)));

        assertEquals(stationInformationRequestsBeforeEndpointCall, stationInformationRequestCount.get());
        assertEquals(stationStatusRequestsBeforeEndpointCall, stationStatusRequestCount.get());
    }

    @Test
    void shouldUpdateReturnedBikeStationsAfterRefresh() throws Exception {
        stationInformationResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "veturilo-3",
                        "name": "Nowa Stacja",
                        "lat": 52.111111,
                        "lon": 21.222222,
                        "capacity": 20
                      }
                    ]
                  }
                }
                """);

        stationStatusResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "veturilo-3",
                        "num_bikes_available": 11,
                        "num_docks_available": 9,
                        "is_installed": true,
                        "is_renting": true,
                        "is_returning": true
                      }
                    ]
                  }
                }
                """);

        veturiloBikeStationService.refreshBikeStations();

        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(1)))
                .andExpect(jsonPath("$.stations[0].stationId", is("veturilo-3")))
                .andExpect(jsonPath("$.stations[0].name", is("Nowa Stacja")))
                .andExpect(jsonPath("$.stations[0].latitude", closeTo(52.111111, 0.000001)))
                .andExpect(jsonPath("$.stations[0].longitude", closeTo(21.222222, 0.000001)))
                .andExpect(jsonPath("$.stations[0].availableBikes", is(11)));
    }

    @Test
    void shouldReturnEmptyListWhenVeturiloApiReturnsEmptyStations() throws Exception {
        stationInformationResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": []
                  }
                }
                """);

        stationStatusResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": []
                  }
                }
                """);

        veturiloBikeStationService.refreshBikeStations();

        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(0)));
    }

    @Test
    void shouldSkipStationsWithoutRequiredDataOrStatus() throws Exception {
        stationInformationResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "valid-station",
                        "name": "Poprawna stacja",
                        "lat": 52.239560,
                        "lon": 21.001779
                      },
                      {
                        "station_id": "station-without-name",
                        "name": null,
                        "lat": 52.200000,
                        "lon": 21.000000
                      },
                      {
                        "station_id": "station-without-latitude",
                        "name": "Brak latitude",
                        "lat": null,
                        "lon": 21.000000
                      },
                      {
                        "station_id": "station-without-status",
                        "name": "Brak statusu",
                        "lat": 52.250000,
                        "lon": 21.050000
                      }
                    ]
                  }
                }
                """);

        stationStatusResponseBody.set("""
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "valid-station",
                        "num_bikes_available": 5
                      }
                    ]
                  }
                }
                """);

        veturiloBikeStationService.refreshBikeStations();

        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(1)))
                .andExpect(jsonPath("$.stations[0].stationId", is("valid-station")))
                .andExpect(jsonPath("$.stations[0].name", is("Poprawna stacja")))
                .andExpect(jsonPath("$.stations[0].availableBikes", is(5)));
    }

    @Test
    void shouldKeepLastValidBikeStationsWhenVeturiloApiFails() throws Exception {
        stationStatusStatusCode.set(500);
        stationStatusResponseBody.set("""
                {
                  "error": "temporary error"
                }
                """);

        veturiloBikeStationService.refreshBikeStations();

        mockMvc.perform(get("/api/bike-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(2)))
                .andExpect(jsonPath("$.stations[0].stationId", is("veturilo-1")))
                .andExpect(jsonPath("$.stations[0].name", is("Metro Centrum")))
                .andExpect(jsonPath("$.stations[1].stationId", is("veturilo-2")))
                .andExpect(jsonPath("$.stations[1].name", is("Rondo ONZ")));
    }

    private static void startFakeVeturiloApiServer() {
        if (fakeVeturiloApiServer != null) {
            return;
        }

        try {
            fakeVeturiloApiServer = HttpServer.create(new InetSocketAddress(FAKE_VETURILO_API_PORT), 0);

            fakeVeturiloApiServer.createContext("/station_information.json", exchange -> {
                stationInformationRequestCount.incrementAndGet();
                lastStationInformationRequestMethod.set(exchange.getRequestMethod());

                byte[] response = stationInformationResponseBody.get().getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(stationInformationStatusCode.get(), response.length);

                try (var responseBody = exchange.getResponseBody()) {
                    responseBody.write(response);
                }
            });

            fakeVeturiloApiServer.createContext("/station_status.json", exchange -> {
                stationStatusRequestCount.incrementAndGet();
                lastStationStatusRequestMethod.set(exchange.getRequestMethod());

                byte[] response = stationStatusResponseBody.get().getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(stationStatusStatusCode.get(), response.length);

                try (var responseBody = exchange.getResponseBody()) {
                    responseBody.write(response);
                }
            });

            fakeVeturiloApiServer.start();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String twoStationsInformationResponse() {
        return """
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "veturilo-1",
                        "name": "Metro Centrum",
                        "lat": 52.231111,
                        "lon": 21.011111,
                        "capacity": 15,
                        "rental_methods": ["phone"]
                      },
                      {
                        "station_id": "veturilo-2",
                        "name": "Rondo ONZ",
                        "lat": 52.233333,
                        "lon": 20.999999,
                        "capacity": 12,
                        "rental_methods": ["phone"]
                      }
                    ]
                  }
                }
                """;
    }

    private static String twoStationsStatusResponse() {
        return """
                {
                  "last_updated": 1782667000,
                  "ttl": 60,
                  "version": "2.3",
                  "data": {
                    "stations": [
                      {
                        "station_id": "veturilo-1",
                        "num_bikes_available": 7,
                        "num_docks_available": 8,
                        "is_installed": true,
                        "is_renting": true,
                        "is_returning": true,
                        "last_reported": 1782666990,
                        "vehicle_types_available": [
                          {
                            "vehicle_type_id": "bike",
                            "count": 7
                          }
                        ]
                      },
                      {
                        "station_id": "veturilo-2",
                        "num_bikes_available": 3,
                        "num_docks_available": 9,
                        "is_installed": true,
                        "is_renting": true,
                        "is_returning": true,
                        "last_reported": 1782666990,
                        "vehicle_types_available": [
                          {
                            "vehicle_type_id": "bike",
                            "count": 3
                          }
                        ]
                      }
                    ]
                  }
                }
                """;
    }
}