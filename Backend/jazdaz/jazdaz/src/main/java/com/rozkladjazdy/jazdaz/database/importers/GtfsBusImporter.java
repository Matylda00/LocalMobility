package com.rozkladjazdy.jazdaz.database.importers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class GtfsBusImporter implements CommandLineRunner {

    private static final int BATCH_SIZE = 5000;
    private static final int GTFS_ROUTE_TYPE_BUS = 3;

    private final JdbcTemplate jdbcTemplate;

    @Value("${gtfs.import.enabled:false}")
    private boolean importEnabled;

    @Value("${gtfs.import.path:}")
    private String gtfsPath;

    private final HashSet<String> busRouteExternalIds = new HashSet<>();
    private final HashSet<String> busTripExternalIds = new HashSet<>();
    private final HashSet<String> busServiceExternalIds = new HashSet<>();
    private final HashSet<String> busStopExternalIds = new HashSet<>();

    private final Map<String, Long> lineIdByExternalId = new HashMap<>();
    private final Map<String, Long> stopIdByExternalId = new HashMap<>();
    private final Map<String, Long> calendarIdByServiceId = new HashMap<>();
    private final Map<String, Long> tripIdByExternalId = new HashMap<>();

    public GtfsBusImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!importEnabled) {
            System.out.println("GTFS import is disabled.");
            return;
        }

        if (gtfsPath == null || gtfsPath.isBlank()) {
            throw new IllegalArgumentException("Property gtfs.import.path is empty.");
        }

        Path folder = Path.of(gtfsPath);

        Path routesFile = folder.resolve("routes.txt");
        Path stopsFile = folder.resolve("stops.txt");
        Path tripsFile = folder.resolve("trips.txt");
        Path stopTimesFile = folder.resolve("stop_times.txt");
        Path calendarDatesFile = folder.resolve("calendar_dates.txt");

        validateRequiredFile(routesFile);
        validateRequiredFile(stopsFile);
        validateRequiredFile(tripsFile);
        validateRequiredFile(stopTimesFile);

        System.out.println("===============================================");
        System.out.println("Starting GTFS BUS-ONLY import");
        System.out.println("Folder: " + folder.toAbsolutePath());
        System.out.println("===============================================");

        clearMemoryCollections();

        cleanBusTables();

        collectBusRoutes(routesFile);
        collectBusTripsAndServices(tripsFile);
        collectBusStopsFromStopTimes(stopTimesFile);

        importBusRoutes(routesFile);
        reloadLineIds();

        importBusServiceCalendars();
        reloadCalendarIds();

        importBusCalendarDates(calendarDatesFile);

        importBusTrips(tripsFile);
        reloadTripIds();

        importBusStops(stopsFile);
        reloadStopIds();

        importBusStopTimes(stopTimesFile);

        printSummary();

        System.out.println("GTFS BUS-ONLY import finished.");
    }

    private void cleanBusTables() {
        System.out.println("Cleaning bus tables...");

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    bus_stop_times,
                    bus_trips,
                    bus_service_calendar_dates,
                    bus_service_calendars,
                    bus_stops,
                    bus_lines
                RESTART IDENTITY
                """);

        System.out.println("Bus tables cleaned. Table users was not touched.");
    }

    private void clearMemoryCollections() {
        busRouteExternalIds.clear();
        busTripExternalIds.clear();
        busServiceExternalIds.clear();
        busStopExternalIds.clear();

        lineIdByExternalId.clear();
        stopIdByExternalId.clear();
        calendarIdByServiceId.clear();
        tripIdByExternalId.clear();
    }

    private void collectBusRoutes(Path routesFile) throws Exception {
        System.out.println("Collecting bus routes from routes.txt...");

        try (CSVParser parser = openCsv(routesFile)) {
            for (CSVRecord record : parser) {
                Integer routeType = getOptionalInt(record, "route_type");

                if (routeType == null || routeType != GTFS_ROUTE_TYPE_BUS) {
                    continue;
                }

                String routeId = record.get("route_id");

                if (routeId != null && !routeId.isBlank()) {
                    busRouteExternalIds.add(routeId);
                }
            }
        }

        System.out.println("Bus routes found: " + busRouteExternalIds.size());

        if (busRouteExternalIds.isEmpty()) {
            throw new IllegalStateException("No bus routes found. Check routes.txt and route_type values.");
        }
    }

    private void collectBusTripsAndServices(Path tripsFile) throws Exception {
        System.out.println("Collecting bus trips from trips.txt...");

        try (CSVParser parser = openCsv(tripsFile)) {
            for (CSVRecord record : parser) {
                String routeId = record.get("route_id");

                if (!busRouteExternalIds.contains(routeId)) {
                    continue;
                }

                String tripId = record.get("trip_id");
                String serviceId = record.get("service_id");

                if (tripId != null && !tripId.isBlank()) {
                    busTripExternalIds.add(tripId);
                }

                if (serviceId != null && !serviceId.isBlank()) {
                    busServiceExternalIds.add(serviceId);
                }
            }
        }

        System.out.println("Bus trips found: " + busTripExternalIds.size());
        System.out.println("Bus services found: " + busServiceExternalIds.size());

        if (busTripExternalIds.isEmpty()) {
            throw new IllegalStateException("No bus trips found. Check trips.txt and route_id values.");
        }
    }

    private void collectBusStopsFromStopTimes(Path stopTimesFile) throws Exception {
        System.out.println("Collecting bus stops from stop_times.txt...");
        System.out.println("This scans stop_times.txt. It may take a while.");

        long allRows = 0;
        long busRows = 0;

        try (CSVParser parser = openCsv(stopTimesFile)) {
            for (CSVRecord record : parser) {
                allRows++;

                String tripId = record.get("trip_id");

                if (!busTripExternalIds.contains(tripId)) {
                    continue;
                }

                String stopId = record.get("stop_id");

                if (stopId != null && !stopId.isBlank()) {
                    busStopExternalIds.add(stopId);
                }

                busRows++;

                if (allRows % 1_000_000 == 0) {
                    System.out.println("Scanned stop_times rows: " + allRows + ", bus rows: " + busRows);
                }
            }
        }

        System.out.println("Bus stop_times rows found: " + busRows);
        System.out.println("Bus stops used by buses found: " + busStopExternalIds.size());

        if (busStopExternalIds.isEmpty()) {
            throw new IllegalStateException("No bus stops found from stop_times.txt.");
        }
    }

    private void importBusRoutes(Path routesFile) throws Exception {
        System.out.println("Importing bus routes...");

        List<Object[]> batch = new ArrayList<>();

        try (CSVParser parser = openCsv(routesFile)) {
            for (CSVRecord record : parser) {
                String routeId = record.get("route_id");

                if (!busRouteExternalIds.contains(routeId)) {
                    continue;
                }

                String shortName = getOptional(record, "route_short_name");
                String longName = getOptional(record, "route_long_name");
                Integer routeType = getOptionalInt(record, "route_type");

                batch.add(new Object[]{
                        routeId,
                        shortName,
                        longName,
                        routeType
                });

                if (batch.size() >= BATCH_SIZE) {
                    saveBusRoutesBatch(batch);
                    batch.clear();
                }
            }
        }

        saveBusRoutesBatch(batch);
    }

    private void saveBusRoutesBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_lines (
                    external_id,
                    line_number,
                    name,
                    route_type
                )
                VALUES (?, ?, ?, ?)
                """, batch);
    }

    private void importBusServiceCalendars() {
        System.out.println("Importing bus service calendars...");

        List<Object[]> batch = new ArrayList<>();

        for (String serviceId : busServiceExternalIds) {
            batch.add(new Object[]{serviceId});

            if (batch.size() >= BATCH_SIZE) {
                saveBusServiceCalendarsBatch(batch);
                batch.clear();
            }
        }

        saveBusServiceCalendarsBatch(batch);
    }

    private void saveBusServiceCalendarsBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_service_calendars (
                    service_id,
                    monday,
                    tuesday,
                    wednesday,
                    thursday,
                    friday,
                    saturday,
                    sunday
                )
                VALUES (?, false, false, false, false, false, false, false)
                """, batch);
    }

    private void importBusCalendarDates(Path calendarDatesFile) throws Exception {
        if (!Files.exists(calendarDatesFile)) {
            System.out.println("calendar_dates.txt not found. Skipping calendar dates.");
            return;
        }

        System.out.println("Importing calendar_dates.txt only for bus services...");

        List<Object[]> batch = new ArrayList<>();

        try (CSVParser parser = openCsv(calendarDatesFile)) {
            for (CSVRecord record : parser) {
                String serviceId = record.get("service_id");

                if (!busServiceExternalIds.contains(serviceId)) {
                    continue;
                }

                Long calendarId = calendarIdByServiceId.get(serviceId);

                if (calendarId == null) {
                    continue;
                }

                Date date = parseGtfsDate(record.get("date"));
                Integer exceptionType = getRequiredInt(record, "exception_type");

                batch.add(new Object[]{
                        calendarId,
                        date,
                        exceptionType
                });

                if (batch.size() >= BATCH_SIZE) {
                    saveBusCalendarDatesBatch(batch);
                    batch.clear();
                }
            }
        }

        saveBusCalendarDatesBatch(batch);
    }

    private void saveBusCalendarDatesBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_service_calendar_dates (
                    service_calendar_id,
                    date,
                    exception_type
                )
                VALUES (?, ?, ?)
                """, batch);
    }

    private void importBusTrips(Path tripsFile) throws Exception {
        System.out.println("Importing bus trips...");

        List<Object[]> batch = new ArrayList<>();

        try (CSVParser parser = openCsv(tripsFile)) {
            for (CSVRecord record : parser) {
                String tripExternalId = record.get("trip_id");

                if (!busTripExternalIds.contains(tripExternalId)) {
                    continue;
                }

                String routeExternalId = record.get("route_id");
                String serviceExternalId = record.get("service_id");

                Long lineId = lineIdByExternalId.get(routeExternalId);
                Long serviceCalendarId = calendarIdByServiceId.get(serviceExternalId);

                if (lineId == null || serviceCalendarId == null) {
                    continue;
                }

                String headsign = getOptional(record, "trip_headsign");
                Integer directionId = getOptionalInt(record, "direction_id");

                batch.add(new Object[]{
                        tripExternalId,
                        lineId,
                        serviceCalendarId,
                        headsign,
                        directionId
                });

                if (batch.size() >= BATCH_SIZE) {
                    saveBusTripsBatch(batch);
                    batch.clear();
                }
            }
        }

        saveBusTripsBatch(batch);
    }

    private void saveBusTripsBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_trips (
                    external_id,
                    line_id,
                    service_calendar_id,
                    headsign,
                    direction_id
                )
                VALUES (?, ?, ?, ?, ?)
                """, batch);
    }

    private void importBusStops(Path stopsFile) throws Exception {
        System.out.println("Importing only stops used by buses...");

        List<Object[]> batch = new ArrayList<>();

        try (CSVParser parser = openCsv(stopsFile)) {
            for (CSVRecord record : parser) {
                String stopExternalId = record.get("stop_id");

                if (!busStopExternalIds.contains(stopExternalId)) {
                    continue;
                }

                String stopCode = getOptional(record, "stop_code");
                String stopName = record.get("stop_name");
                Double latitude = getRequiredDouble(record, "stop_lat");
                Double longitude = getRequiredDouble(record, "stop_lon");
                String platformCode = getOptional(record, "platform_code");

                batch.add(new Object[]{
                        stopExternalId,
                        stopCode,
                        stopName,
                        latitude,
                        longitude,
                        platformCode
                });

                if (batch.size() >= BATCH_SIZE) {
                    saveBusStopsBatch(batch);
                    batch.clear();
                }
            }
        }

        saveBusStopsBatch(batch);
    }

    private void saveBusStopsBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_stops (
                    external_id,
                    stop_code,
                    name,
                    latitude,
                    longitude,
                    platform_code
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """, batch);
    }

    private void importBusStopTimes(Path stopTimesFile) throws Exception {
        System.out.println("Importing only stop_times for bus trips...");
        System.out.println("This scans stop_times.txt. It may take a while.");

        List<Object[]> batch = new ArrayList<>();

        long allRows = 0;
        long importedRows = 0;

        try (CSVParser parser = openCsv(stopTimesFile)) {
            for (CSVRecord record : parser) {
                allRows++;

                String tripExternalId = record.get("trip_id");

                if (!busTripExternalIds.contains(tripExternalId)) {
                    continue;
                }

                String stopExternalId = record.get("stop_id");

                Long tripId = tripIdByExternalId.get(tripExternalId);
                Long stopId = stopIdByExternalId.get(stopExternalId);

                if (tripId == null || stopId == null) {
                    continue;
                }

                String arrivalTime = record.get("arrival_time");
                String departureTime = record.get("departure_time");
                Integer stopSequence = getRequiredInt(record, "stop_sequence");

                Integer arrivalSeconds = parseGtfsTimeToSeconds(arrivalTime);
                Integer departureSeconds = parseGtfsTimeToSeconds(departureTime);

                batch.add(new Object[]{
                        tripId,
                        stopId,
                        stopSequence,
                        arrivalTime,
                        departureTime,
                        arrivalSeconds,
                        departureSeconds
                });

                importedRows++;

                if (batch.size() >= BATCH_SIZE) {
                    saveBusStopTimesBatch(batch);
                    batch.clear();
                }

                if (allRows % 1_000_000 == 0) {
                    System.out.println("Scanned stop_times rows: " + allRows + ", imported bus rows: " + importedRows);
                }
            }
        }

        saveBusStopTimesBatch(batch);

        System.out.println("Imported bus stop_times rows: " + importedRows);
    }

    private void saveBusStopTimesBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO bus_stop_times (
                    trip_id,
                    stop_id,
                    stop_sequence,
                    arrival_time,
                    departure_time,
                    arrival_seconds,
                    departure_seconds
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, batch);
    }

    private void reloadLineIds() {
        lineIdByExternalId.clear();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, external_id
                FROM bus_lines
                """);

        for (Map<String, Object> row : rows) {
            String externalId = String.valueOf(row.get("external_id"));
            Long id = toLong(row.get("id"));

            if (externalId != null && id != null) {
                lineIdByExternalId.put(externalId, id);
            }
        }
    }

    private void reloadStopIds() {
        stopIdByExternalId.clear();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, external_id
                FROM bus_stops
                """);

        for (Map<String, Object> row : rows) {
            String externalId = String.valueOf(row.get("external_id"));
            Long id = toLong(row.get("id"));

            if (externalId != null && id != null) {
                stopIdByExternalId.put(externalId, id);
            }
        }
    }

    private void reloadCalendarIds() {
        calendarIdByServiceId.clear();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, service_id
                FROM bus_service_calendars
                """);

        for (Map<String, Object> row : rows) {
            String serviceId = String.valueOf(row.get("service_id"));
            Long id = toLong(row.get("id"));

            if (serviceId != null && id != null) {
                calendarIdByServiceId.put(serviceId, id);
            }
        }
    }

    private void reloadTripIds() {
        tripIdByExternalId.clear();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, external_id
                FROM bus_trips
                """);

        for (Map<String, Object> row : rows) {
            String externalId = String.valueOf(row.get("external_id"));
            Long id = toLong(row.get("id"));

            if (externalId != null && id != null) {
                tripIdByExternalId.put(externalId, id);
            }
        }
    }

    private CSVParser openCsv(Path file) throws Exception {
        BufferedReader reader = newBufferedReaderWithoutBom(file);

        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private BufferedReader newBufferedReaderWithoutBom(Path file) throws IOException {
        InputStream inputStream = Files.newInputStream(file);
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 3);

        byte[] bom = new byte[3];
        int bytesRead = pushbackInputStream.read(bom, 0, bom.length);

        boolean hasUtf8Bom = bytesRead == 3
                && (bom[0] & 0xFF) == 0xEF
                && (bom[1] & 0xFF) == 0xBB
                && (bom[2] & 0xFF) == 0xBF;

        if (!hasUtf8Bom && bytesRead > 0) {
            pushbackInputStream.unread(bom, 0, bytesRead);
        }

        return new BufferedReader(new InputStreamReader(pushbackInputStream, StandardCharsets.UTF_8));
    }

    private String getOptional(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            return null;
        }

        String value = record.get(columnName);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Integer getOptionalInt(CSVRecord record, String columnName) {
        String value = getOptional(record, columnName);

        if (value == null) {
            return null;
        }

        return Integer.parseInt(value);
    }

    private Integer getRequiredInt(CSVRecord record, String columnName) {
        String value = record.get(columnName);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required integer column: " + columnName);
        }

        return Integer.parseInt(value.trim());
    }

    private Double getRequiredDouble(CSVRecord record, String columnName) {
        String value = record.get(columnName);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required double column: " + columnName);
        }

        return Double.parseDouble(value.trim());
    }

    private Date parseGtfsDate(String value) {
        if (value == null || value.length() != 8) {
            throw new IllegalArgumentException("Invalid GTFS date: " + value);
        }

        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(4, 6));
        int day = Integer.parseInt(value.substring(6, 8));

        return Date.valueOf(String.format("%04d-%02d-%02d", year, month, day));
    }

    private Integer parseGtfsTimeToSeconds(String time) {
        if (time == null || time.isBlank()) {
            throw new IllegalArgumentException("GTFS time is empty.");
        }

        String[] parts = time.trim().split(":");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time: " + time);
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private void validateRequiredFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Missing required GTFS file: " + file.toAbsolutePath());
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("GTFS path is not a file: " + file.toAbsolutePath());
        }
    }

    private void printSummary() {
        System.out.println("========== GTFS BUS-ONLY IMPORT SUMMARY ==========");
        System.out.println("Bus routes: " + busRouteExternalIds.size());
        System.out.println("Bus trips: " + busTripExternalIds.size());
        System.out.println("Bus services: " + busServiceExternalIds.size());
        System.out.println("Bus stops: " + busStopExternalIds.size());
        System.out.println("==================================================");
    }
}