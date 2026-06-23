package com.rozkladjazdy.jazdaz.database.importers;

import com.rozkladjazdy.jazdaz.database.entities.BusStopEntity;
import com.rozkladjazdy.jazdaz.database.repositories.BusStopRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Order(2)
public class WarsawBusStopsGtfsImporter implements CommandLineRunner {

    private static final String GTFS_URL = "https://mkuran.pl/gtfs/warsaw.zip";

    private final BusStopRepository busStopRepository;

    public WarsawBusStopsGtfsImporter(BusStopRepository busStopRepository) {
        this.busStopRepository = busStopRepository;
    }

    @Override
    public void run( String @NonNull ... args) throws Exception {
        importBusStops();
    }

    private void importBusStops() throws Exception {
        URL url = URI.create(GTFS_URL).toURL();

        try (
                InputStream inputStream = url.openStream();
                ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("stops.txt".equals(entry.getName())) {
                    importStopsFile(zipInputStream);
                    return;
                }
            }
        }
    }

    private void importStopsFile(InputStream stopsInputStream) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stopsInputStream, StandardCharsets.UTF_8)
        );

        String headerLine = reader.readLine();

        if (headerLine == null) {
            return;
        }

        String[] headers = splitCsvLine(headerLine);
        Map<String, Integer> headerIndexes = createHeaderIndexes(headers);

        String line;
        int imported = 0;
        int skipped = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = splitCsvLine(line);

            String stopId = getValue(values, headerIndexes, "stop_id");
            String stopCode = getValue(values, headerIndexes, "stop_code");
            String stopName = getValue(values, headerIndexes, "stop_name");
            String stopLat = getValue(values, headerIndexes, "stop_lat");
            String stopLon = getValue(values, headerIndexes, "stop_lon");
            String platformCode = getValue(values, headerIndexes, "platform_code");

            if (stopId == null || stopName == null || stopLat == null || stopLon == null) {
                skipped++;
                continue;
            }

            BusStopEntity busStop = busStopRepository
                    .findByExternalId(stopId)
                    .orElseGet(BusStopEntity::new);

            busStop.setExternalId(stopId);
            busStop.setStopCode(stopCode);
            busStop.setName(stopName);
            busStop.setLatitude(new BigDecimal(stopLat));
            busStop.setLongitude(new BigDecimal(stopLon));
            busStop.setPlatformCode(platformCode);

            busStopRepository.save(busStop);
            imported++;
        }

        System.out.println("GTFS bus stops imported/updated: " + imported);
        System.out.println("GTFS bus stops skipped: " + skipped);
    }

    private Map<String, Integer> createHeaderIndexes(String[] headers) {
        Map<String, Integer> indexes = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            indexes.put(headers[i].trim(), i);
        }

        return indexes;
    }

    private String getValue(String[] values, Map<String, Integer> indexes, String columnName) {
        Integer index = indexes.get(columnName);

        if (index == null || index >= values.length) {
            return null;
        }

        String value = values[index];

        if (value == null) {
            return null;
        }

        value = value.trim();

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }

        if (value.isBlank()) {
            return null;
        }

        return value;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}