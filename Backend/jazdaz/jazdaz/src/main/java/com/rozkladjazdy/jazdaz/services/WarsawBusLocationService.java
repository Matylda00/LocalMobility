package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.dtos.BusLocationDto;
import com.rozkladjazdy.jazdaz.dtos.BusLocationResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WarsawBusLocationService {

    private final RestClient restClient = RestClient.create();

    @Value("${warsaw.bus-locations.url}")
    private String url;

    @Value("${warsaw.bus-locations.api-key}")
    private String apiKey;

    private volatile BusLocationResponse currentBusLocations =
            new BusLocationResponse(List.of());

    public BusLocationResponse getBusLocations() {
        return currentBusLocations;
    }

    @PostConstruct
    public void init() {
        refreshBusLocations();
    }

    @Scheduled(fixedRateString = "${warsaw.bus-locations.refresh-ms}")
    public void refreshBusLocations() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            WarsawVehicleLocation[] response = restClient.post()
                    .uri(url)
                    .header("Authorization", apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", 1))
                    .retrieve()
                    .body(WarsawVehicleLocation[].class);

            if (response == null) {
                return;
            }

            List<BusLocationDto> vehicles = Arrays.stream(response)
                    .filter(vehicle -> vehicle.Lines() != null)
                    .filter(vehicle -> vehicle.Lat() != null)
                    .filter(vehicle -> vehicle.Lon() != null)
                    .map(vehicle -> new BusLocationDto(
                            vehicle.Lines(),
                            vehicle.Lat(),
                            vehicle.Lon()
                    ))
                    .toList();

            currentBusLocations = new BusLocationResponse(vehicles);

        } catch (Exception exception) {
            System.out.println("Nie udało się pobrać lokalizacji autobusów: " + exception.getMessage());
        }
    }

    private record WarsawVehicleLocation(
            String Brigade,
            String Lines,
            Double Lat,
            Double Lon,
            String Time,
            String VehicleNumber
    ) {
    }
}