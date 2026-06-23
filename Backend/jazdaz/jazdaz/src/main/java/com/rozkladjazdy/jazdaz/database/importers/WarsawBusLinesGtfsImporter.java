package com.rozkladjazdy.jazdaz.database.importers;

import com.rozkladjazdy.jazdaz.database.entities.BusLineEntity;
import com.rozkladjazdy.jazdaz.database.repositories.BusLineRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Order(1)
public class WarsawBusLinesGtfsImporter implements CommandLineRunner {

    private static final String GTFS_URL = "https://mkuran.pl/gtfs/warsaw.zip";

    private final BusLineRepository busLineRepository;

    public WarsawBusLinesGtfsImporter(BusLineRepository busLineRepository) {
        this.busLineRepository = busLineRepository;
    }

    @Override
    public void run( String @NonNull ... args) throws Exception {
        importBusLines();
    }

    private void importBusLines() throws Exception {
        URL url = URI.create(GTFS_URL).toURL();

        try (
                InputStream inputStream = url.openStream();
                ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("routes.txt".equals(entry.getName())) {
                    importRoutesFile(zipInputStream);
                    return;
                }
            }
        }
    }

    private void importRoutesFile(InputStream routesInputStream) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(routesInputStream, StandardCharsets.UTF_8)
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

            String routeId = getValue(values, headerIndexes, "route_id");
            String routeShortName = getValue(values, headerIndexes, "route_short_name");
            String routeLongName = getValue(values, headerIndexes, "route_long_name");
            String routeTypeValue = getValue(values, headerIndexes, "route_type");

            if (routeId == null || routeShortName == null || routeTypeValue == null) {
                skipped++;
                continue;
            }

            int routeType = Integer.parseInt(routeTypeValue);

            // GTFS: 3 = bus
            if (routeType != 3) {
                skipped++;
                continue;
            }

            BusLineEntity busLine = busLineRepository
                    .findByExternalId(routeId)
                    .orElseGet(BusLineEntity::new);

            busLine.setExternalId(routeId);
            busLine.setLineNumber(routeShortName);
            busLine.setName(
                    routeLongName == null || routeLongName.isBlank()
                            ? "Linia " + routeShortName
                            : routeLongName
            );
            busLine.setRouteType(routeType);

            busLineRepository.save(busLine);
            imported++;
        }

        System.out.println("GTFS bus lines imported/updated: " + imported);
        System.out.println("GTFS routes skipped: " + skipped);
    }

    private Map<String, Integer> createHeaderIndexes(String[] headers) {
        Map<String, Integer> indexes = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            indexes.put(headers[i], i);
        }

        return indexes;
    }

    private String getValue(String[] values, Map<String, Integer> indexes, String columnName) {
        Integer index = indexes.get(columnName);

        if (index == null || index >= values.length) {
            return null;
        }

        String value = values[index];

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String[] splitCsvLine(String line) {
        // Prosty parser CSV wystarczy dla routes.txt w GTFS.
        // Jeśli potem będziemy importować stop_times, lepiej dodać Apache Commons CSV.
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}