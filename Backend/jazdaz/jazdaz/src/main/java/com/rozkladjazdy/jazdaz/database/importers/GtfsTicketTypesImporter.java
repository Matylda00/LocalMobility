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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class GtfsTicketTypesImporter implements CommandLineRunner {

    private static final int BATCH_SIZE = 1000;
    private static final int LONG_TERM_MINUTES_THRESHOLD = 30 * 24 * 60;

    private final JdbcTemplate jdbcTemplate;

    @Value("${gtfs.import.enabled:false}")
    private boolean importEnabled;

    @Value("${gtfs.import.path:}")
    private String gtfsPath;

    public GtfsTicketTypesImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!importEnabled) {
            System.out.println("Ticket types import is disabled.");
            return;
        }

        if (gtfsPath == null || gtfsPath.isBlank()) {
            throw new IllegalArgumentException("Property gtfs.import.path is empty.");
        }

        Path folder = Path.of(gtfsPath);
        Path fareAttributesFile = folder.resolve("fare_attributes.txt");

        validateRequiredFile(fareAttributesFile);

        System.out.println("===============================================");
        System.out.println("Starting GTFS ticket types import");
        System.out.println("Folder: " + folder.toAbsolutePath());
        System.out.println("File: " + fareAttributesFile.toAbsolutePath());
        System.out.println("===============================================");

        cleanTicketTypesTable();
        importTicketTypes(fareAttributesFile);

        System.out.println("GTFS ticket types import finished.");
    }

    private void cleanTicketTypesTable() {
        System.out.println("Cleaning tickets and ticket_types tables...");

        jdbcTemplate.execute("""
            TRUNCATE TABLE tickets, ticket_types RESTART IDENTITY
            """);

        System.out.println("tickets and ticket_types tables cleaned.");
    }

    private void importTicketTypes(Path fareAttributesFile) throws Exception {
        System.out.println("Importing ticket types from fare_attributes.txt...");

        List<Object[]> batch = new ArrayList<>();
        long importedRows = 0;
        long skippedRows = 0;

        try (CSVParser parser = openCsv(fareAttributesFile)) {
            for (CSVRecord record : parser) {
                String fareId = getRequired(record, "fare_id");
                BigDecimal price = getRequiredBigDecimal(record, "price");
                String currencyType = getOptional(record, "currency_type");

                if (currencyType != null && !currencyType.equalsIgnoreCase("PLN")) {
                    skippedRows++;
                    continue;
                }

                Integer durationMinutes = getDurationMinutes(record);
                String ticketCategory = getTicketCategory(durationMinutes);

                batch.add(new Object[]{
                        fareId,
                        price,
                        durationMinutes,
                        ticketCategory
                });

                importedRows++;

                if (batch.size() >= BATCH_SIZE) {
                    saveTicketTypesBatch(batch);
                    batch.clear();
                }
            }
        }

        saveTicketTypesBatch(batch);

        System.out.println("Imported ticket types rows: " + importedRows);
        System.out.println("Skipped ticket types rows: " + skippedRows);
    }

    private void saveTicketTypesBatch(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO ticket_types (
                    name,
                    price,
                    duration_minutes,
                    ticket_category
                )
                VALUES (?, ?, ?, ?)
                """, batch);
    }

    private Integer getDurationMinutes(CSVRecord record) {
        Integer transferDurationSeconds = getOptionalInt(record, "transfer_duration");

        if (transferDurationSeconds == null || transferDurationSeconds <= 0) {
            return null;
        }

        return transferDurationSeconds / 60;
    }

    private String getTicketCategory(Integer durationMinutes) {
        if (durationMinutes == null) {
            return null;
        }

        if (durationMinutes >= LONG_TERM_MINUTES_THRESHOLD) {
            return "LONG_TERM";
        }

        return "TIME";
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

    private String getRequired(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            throw new IllegalArgumentException("Missing required column: " + columnName);
        }

        String value = record.get(columnName);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required value for column: " + columnName);
        }

        return value.trim();
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

    private BigDecimal getRequiredBigDecimal(CSVRecord record, String columnName) {
        return new BigDecimal(getRequired(record, columnName));
    }

    private void validateRequiredFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Missing required GTFS file: " + file.toAbsolutePath());
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("GTFS path is not a file: " + file.toAbsolutePath());
        }
    }
}