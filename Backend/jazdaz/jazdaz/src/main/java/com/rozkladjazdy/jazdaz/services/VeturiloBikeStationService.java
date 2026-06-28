package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.dtos.BikeStationDto;
import com.rozkladjazdy.jazdaz.dtos.BikeStationResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VeturiloBikeStationService {

    private final RestClient restClient = RestClient.create();

    @Value("${veturilo.station-information.url}")
    private String stationInformationUrl;

    @Value("${veturilo.station-status.url}")
    private String stationStatusUrl;

    private volatile BikeStationResponse currentBikeStations =
            new BikeStationResponse(List.of());

    public BikeStationResponse getBikeStations() {
        return currentBikeStations;
    }

    @PostConstruct
    public void init() {
        refreshBikeStations();
    }

    @Scheduled(fixedRateString = "${veturilo.bike-stations.refresh-ms}")
    public void refreshBikeStations() {
        try {
            VeturiloStationInformationResponse informationResponse = restClient.get()
                    .uri(stationInformationUrl)
                    .retrieve()
                    .body(VeturiloStationInformationResponse.class);

            VeturiloStationStatusResponse statusResponse = restClient.get()
                    .uri(stationStatusUrl)
                    .retrieve()
                    .body(VeturiloStationStatusResponse.class);

            if (informationResponse == null
                    || informationResponse.data() == null
                    || informationResponse.data().stations() == null
                    || statusResponse == null
                    || statusResponse.data() == null
                    || statusResponse.data().stations() == null) {
                return;
            }

            Map<String, VeturiloStationStatus> statusesByStationId =
                    statusResponse.data().stations().stream()
                            .filter(status -> status.station_id() != null)
                            .collect(Collectors.toMap(
                                    VeturiloStationStatus::station_id,
                                    Function.identity(),
                                    (first, second) -> first
                            ));

            List<BikeStationDto> stations = informationResponse.data().stations().stream()
                    .filter(station -> station.station_id() != null)
                    .filter(station -> station.name() != null)
                    .filter(station -> station.lat() != null)
                    .filter(station -> station.lon() != null)
                    .map(station -> {
                        VeturiloStationStatus status = statusesByStationId.get(station.station_id());

                        if (status == null) {
                            return null;
                        }

                        return new BikeStationDto(
                                station.station_id(),
                                station.name(),
                                station.lat(),
                                station.lon(),
                                status.num_bikes_available() == null ? 0 : status.num_bikes_available()
                        );
                    })
                    .filter(Objects::nonNull)
                    .toList();

            currentBikeStations = new BikeStationResponse(stations);
        } catch (Exception exception) {
            System.out.println("Nie udało się pobrać stacji rowerowych Veturilo: " + exception.getMessage());
        }
    }

    private record VeturiloStationInformationResponse(
            VeturiloStationInformationData data
    ) {}

    private record VeturiloStationInformationData(
            List<VeturiloStationInformation> stations
    ) {}

    private record VeturiloStationInformation(
            String station_id,
            String name,
            Double lat,
            Double lon
    ) {}

    private record VeturiloStationStatusResponse(
            VeturiloStationStatusData data
    ) {}

    private record VeturiloStationStatusData(
            List<VeturiloStationStatus> stations
    ) {}

    private record VeturiloStationStatus(
            String station_id,
            Integer num_bikes_available
    ) {}
}