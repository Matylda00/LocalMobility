package com.rozkladjazdy.jazdaz.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rozkladjazdy.jazdaz.dtos.ParkingDto;
import com.rozkladjazdy.jazdaz.dtos.ParkingResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Service
public class WarsawParkingService {

    private final RestClient restClient = RestClient.create();

    @Value("${warsaw.parkings.url}")
    private String url;

    @Value("${warsaw.parkings.api-key}")
    private String apiKey;

    private volatile ParkingResponse currentParkings = new ParkingResponse(List.of());

    public ParkingResponse getParkings() {
        return currentParkings;
    }

    @PostConstruct
    public void init() {
        refreshParkings();
    }

    @Scheduled(fixedRateString = "${warsaw.parkings.refresh-ms}")
    public void refreshParkings() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            WarsawParkingResponse response = restClient.post()
                    .uri(url)
                    .header("Authorization", apiKey.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(WarsawParkingResponse.class);

            if (response == null || response.carParks() == null) {
                return;
            }

            List<ParkingDto> parkings = Arrays.stream(response.carParks())
                    .filter(parking -> parking.name() != null)
                    .filter(parking -> parking.latitude() != null)
                    .filter(parking -> parking.longitude() != null)
                    .filter(parking -> parking.freePlacesTotal() != null)
                    .map(parking -> new ParkingDto(
                            parking.name(),
                            Double.parseDouble(parking.latitude()),
                            Double.parseDouble(parking.longitude()),
                            parking.freePlacesTotal().availableSpaces()
                    ))
                    .toList();

            currentParkings = new ParkingResponse(parkings);
        } catch (Exception exception) {
            System.out.println("Nie udało się pobrać lokalizacji parkingów: " + exception.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WarsawParkingResponse(
            @JsonProperty("carParks")
            WarsawParking[] carParks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WarsawParking(
            String name,
            String latitude,
            String longitude,

            @JsonProperty("free_places_total")
            FreePlacesTotal freePlacesTotal
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FreePlacesTotal(
            @JsonProperty("public")
            Integer publicPlaces,

            Integer disabled,
            Integer electric
    ) {
        private int availableSpaces() {
            return value(publicPlaces) + value(disabled) + value(electric);
        }

        private int value(Integer number) {
            return number == null ? 0 : number;
        }
    }
}