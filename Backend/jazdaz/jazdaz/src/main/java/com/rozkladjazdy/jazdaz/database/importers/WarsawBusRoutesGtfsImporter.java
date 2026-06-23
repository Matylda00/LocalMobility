package com.rozkladjazdy.jazdaz.database.importers;

import com.rozkladjazdy.jazdaz.database.entities.BusLineEntity;
import com.rozkladjazdy.jazdaz.database.entities.BusRouteEntity;
import com.rozkladjazdy.jazdaz.database.entities.BusRouteStopEntity;
import com.rozkladjazdy.jazdaz.database.entities.BusStopEntity;
import com.rozkladjazdy.jazdaz.database.repositories.BusLineRepository;
import com.rozkladjazdy.jazdaz.database.repositories.BusRouteRepository;
import com.rozkladjazdy.jazdaz.database.repositories.BusStopRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Order(3)
public class WarsawBusRoutesGtfsImporter implements CommandLineRunner {

    private static final String GTFS_URL = "https://mkuran.pl/gtfs/warsaw.zip";

    private final BusLineRepository busLineRepository;
    private final BusStopRepository busStopRepository;
    private final BusRouteRepository busRouteRepository;

    public WarsawBusRoutesGtfsImporter(
            BusLineRepository busLineRepository,
            BusStopRepository busStopRepository,
            BusRouteRepository busRouteRepository
    ) {
        this.busLineRepository = busLineRepository;
        this.busStopRepository = busStopRepository;
        this.busRouteRepository = busRouteRepository;
    }

    @Override
    @Transactional
    public void run( String @NonNull ... args) throws Exception {

        //importBusRoutes();
    }

    private void importBusRoutes() throws Exception {
        System.out.println("Starting importing files");
        Map<String, GtfsFile> files = readGtfsFiles();

        System.out.println("Done reading files, parsing starts");
        GtfsFile routesFile = files.get("routes.txt");
        GtfsFile tripsFile = files.get("trips.txt");
        GtfsFile stopTimesFile = files.get("stop_times.txt");

        if (routesFile == null || tripsFile == null || stopTimesFile == null) {
            System.out.println("GTFS bus routes import skipped: missing routes.txt, trips.txt or stop_times.txt");
            return;
        }

        Set<String> busRouteIds = readBusRouteIds(routesFile);
        Map<String, TripCandidate> representativeTrips = readRepresentativeTrips(tripsFile, busRouteIds);
        Map<String, List<RouteStopCandidate>> stopsByTripId =
                readStopsForRepresentativeTrips(stopTimesFile, representativeTrips);

        int importedRoutes = 0;
        int skippedRoutes = 0;
        int importedStops = 0;
        int skippedStops = 0;

        for (TripCandidate trip : representativeTrips.values()) {
            Optional<BusLineEntity> optionalLine = busLineRepository.findByExternalId(trip.routeId());

            if (optionalLine.isEmpty()) {
                skippedRoutes++;
                continue;
            }

            BusRouteEntity route = busRouteRepository
                    .findByExternalId(trip.patternKey())
                    .orElseGet(BusRouteEntity::new);

            route.setExternalId(trip.patternKey());
            route.setLine(optionalLine.get());
            route.setDirectionId(trip.directionId());
            route.setHeadsign(trip.headsign());
            route.setShapeId(trip.shapeId());
            route.setGtfsTripId(trip.tripId());

            route.clearStops();

            List<RouteStopCandidate> routeStops =
                    stopsByTripId.getOrDefault(trip.tripId(), List.of());

            routeStops.sort(Comparator.comparingInt(RouteStopCandidate::stopSequence));

            for (RouteStopCandidate routeStopCandidate : routeStops) {
                Optional<BusStopEntity> optionalStop =
                        busStopRepository.findByExternalId(routeStopCandidate.stopId());

                if (optionalStop.isEmpty()) {
                    skippedStops++;
                    continue;
                }

                BusRouteStopEntity routeStop = new BusRouteStopEntity();
                routeStop.setStop(optionalStop.get());
                routeStop.setStopSequence(routeStopCandidate.stopSequence());

                route.addStop(routeStop);
                importedStops++;
            }

            busRouteRepository.save(route);
            importedRoutes++;
        }

        System.out.println("GTFS bus routes imported/updated: " + importedRoutes);
        System.out.println("GTFS bus routes skipped: " + skippedRoutes);
        System.out.println("GTFS bus route stops imported: " + importedStops);
        System.out.println("GTFS bus route stops skipped: " + skippedStops);
    }

    private Map<String, GtfsFile> readGtfsFiles() throws Exception {
        URL url = URI.create(GTFS_URL).toURL();
        Map<String, GtfsFile> files = new HashMap<>();

        try (
                InputStream inputStream = url.openStream();
                ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (
                        "routes.txt".equals(entryName)
                                || "trips.txt".equals(entryName)
                                || "stop_times.txt".equals(entryName)
                ) {
                    files.put(entryName, readGtfsFile(zipInputStream));
                }
            }
        }

        return files;
    }

    private GtfsFile readGtfsFile(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        String headerLine = reader.readLine();

        if (headerLine == null) {
            return new GtfsFile(Map.of(), List.of());
        }

        String[] headers = splitCsvLine(headerLine);
        Map<String, Integer> headerIndexes = createHeaderIndexes(headers);
        List<String[]> rows = new ArrayList<>();

        String line;

        while ((line = reader.readLine()) != null) {
            rows.add(splitCsvLine(line));
        }

        return new GtfsFile(headerIndexes, rows);
    }

    private Set<String> readBusRouteIds(GtfsFile routesFile) {
        Set<String> busRouteIds = new HashSet<>();

        for (String[] row : routesFile.rows()) {
            String routeId = getValue(row, routesFile.headerIndexes(), "route_id");
            String routeTypeValue = getValue(row, routesFile.headerIndexes(), "route_type");

            if (routeId == null || routeTypeValue == null) {
                continue;
            }

            if (Integer.parseInt(routeTypeValue) == 3) {
                busRouteIds.add(routeId);
            }
        }

        return busRouteIds;
    }

    private Map<String, TripCandidate> readRepresentativeTrips(
            GtfsFile tripsFile,
            Set<String> busRouteIds
    ) {
        Map<String, TripCandidate> representativeTrips = new LinkedHashMap<>();

        for (String[] row : tripsFile.rows()) {
            String routeId = getValue(row, tripsFile.headerIndexes(), "route_id");
            String tripId = getValue(row, tripsFile.headerIndexes(), "trip_id");
            String headsign = getValue(row, tripsFile.headerIndexes(), "trip_headsign");
            String directionIdValue = getValue(row, tripsFile.headerIndexes(), "direction_id");
            String shapeId = getValue(row, tripsFile.headerIndexes(), "shape_id");

            if (routeId == null || tripId == null || !busRouteIds.contains(routeId)) {
                continue;
            }

            Integer directionId = directionIdValue == null ? null : Integer.parseInt(directionIdValue);

            String patternKey = routeId
                    + "|direction=" + nullToEmpty(directionIdValue)
                    + "|shape=" + nullToEmpty(shapeId)
                    + "|headsign=" + nullToEmpty(headsign);

            representativeTrips.putIfAbsent(
                    patternKey,
                    new TripCandidate(patternKey, routeId, tripId, headsign, directionId, shapeId)
            );
        }

        return representativeTrips;
    }

    private Map<String, List<RouteStopCandidate>> readStopsForRepresentativeTrips(
            GtfsFile stopTimesFile,
            Map<String, TripCandidate> representativeTrips
    ) {
        Map<String, TripCandidate> tripIdToCandidate = new HashMap<>();

        for (TripCandidate candidate : representativeTrips.values()) {
            tripIdToCandidate.put(candidate.tripId(), candidate);
        }

        Map<String, List<RouteStopCandidate>> stopsByTripId = new HashMap<>();

        int processedRows = 0;
        int matchedRows = 0;

        for (String[] row : stopTimesFile.rows()) {
            processedRows++;

            if (processedRows % 100_000 == 0) {
                System.out.println("GTFS stop_times processed rows: " + processedRows
                        + ", matched rows: " + matchedRows);
            }

            String tripId = getValue(row, stopTimesFile.headerIndexes(), "trip_id");

            if (tripId == null || !tripIdToCandidate.containsKey(tripId)) {
                continue;
            }

            String stopId = getValue(row, stopTimesFile.headerIndexes(), "stop_id");
            String stopSequenceValue = getValue(row, stopTimesFile.headerIndexes(), "stop_sequence");

            if (stopId == null || stopSequenceValue == null) {
                continue;
            }

            stopsByTripId
                    .computeIfAbsent(tripId, key -> new ArrayList<>())
                    .add(new RouteStopCandidate(stopId, Integer.parseInt(stopSequenceValue)));

            matchedRows++;
        }

        System.out.println("GTFS stop_times finished. Processed rows: " + processedRows
                + ", matched rows: " + matchedRows);

        return stopsByTripId;
    }

    private Map<String, Integer> createHeaderIndexes(String[] headers) {
        Map<String, Integer> indexes = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            indexes.put(cleanCsvValue(headers[i]), i);
        }

        return indexes;
    }

    private String getValue(String[] values, Map<String, Integer> indexes, String columnName) {
        Integer index = indexes.get(columnName);

        if (index == null || index >= values.length) {
            return null;
        }

        String value = cleanCsvValue(values[index]);

        if (value.isBlank()) {
            return null;
        }

        return value;
    }

    private String cleanCsvValue(String value) {
        if (value == null) {
            return "";
        }

        value = value.trim();

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private record GtfsFile(
            Map<String, Integer> headerIndexes,
            List<String[]> rows
    ) {
    }

    private record TripCandidate(
            String patternKey,
            String routeId,
            String tripId,
            String headsign,
            Integer directionId,
            String shapeId
    ) {
    }

    private record RouteStopCandidate(
            String stopId,
            int stopSequence
    ) {
    }
}