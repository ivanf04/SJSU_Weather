package com.weather.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalCsvRepositoryTest
 *
 * LocalCsvRepository does real file I/O. Every test uses an isolated temp file
 * that is deleted in @AfterEach so tests never bleed into each other.
 *
 * We test:
 *   1. getLastTimestamp() returns ""  when file does not exist
 *   2. getFirstTimestamp() returns "" when file does not exist
 *   3. append() creates the file and writes the header on first call
 *   4. append() does NOT write the header again on a second call
 *   5. getLastTimestamp() returns the timestamp from the last appended row
 *   6. getFirstTimestamp() returns the timestamp from the first data row
 *   7. append() with an empty record list writes nothing beyond the header
 *   8. Timestamps are parsed correctly even when fields contain quotes
 */
class LocalCsvRepositoryTest {

    private static final String TEST_FILE = "test_weather_repo.csv";
    private static final String[] HEADERS = { "STATION", "DATE", "TIMESTAMP", "TEMP", "HUMIDITY" };

    private LocalCsvRepository repo;

    @BeforeEach
    void setUp() {
        repo = new LocalCsvRepository(TEST_FILE);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Paths.get(TEST_FILE));
    }

    // -------------------------------------------------------------------------
    // 1 & 2 — Missing file
    // -------------------------------------------------------------------------

    @Test
    void getLastTimestamp_returnsEmpty_whenFileDoesNotExist() {
        assertEquals("", repo.getLastTimestamp(),
                "Should return empty string when file is missing");
    }

    @Test
    void getFirstTimestamp_returnsEmpty_whenFileDoesNotExist() {
        assertEquals("", repo.getFirstTimestamp(),
                "Should return empty string when file is missing");
    }

    // -------------------------------------------------------------------------
    // 3 — First append creates file and writes header
    // -------------------------------------------------------------------------

    @Test
    void append_createsFileWithHeader_onFirstCall() throws Exception {
        repo.append(List.of(makeRecord("2026-04-22 08:00", "15.0", "65")), HEADERS);

        assertTrue(Files.exists(Paths.get(TEST_FILE)), "File should be created after append");

        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_FILE))) {
            String firstLine = reader.readLine();
            assertEquals(String.join(",", HEADERS), firstLine,
                    "First line should be the header row");
        }
    }

    // -------------------------------------------------------------------------
    // 4 — Second append does NOT duplicate the header
    // -------------------------------------------------------------------------

    @Test
    void append_doesNotWriteHeaderTwice_onSecondCall() throws Exception {
        repo.append(List.of(makeRecord("2026-04-22 08:00", "15.0", "65")), HEADERS);
        repo.append(List.of(makeRecord("2026-04-22 09:00", "16.0", "63")), HEADERS);

        long headerCount = Files.lines(Paths.get(TEST_FILE))
                .filter(line -> line.equals(String.join(",", HEADERS)))
                .count();

        assertEquals(1, headerCount, "Header should appear exactly once across multiple appends");
    }

    // -------------------------------------------------------------------------
    // 5 — getLastTimestamp() reads the final row
    // -------------------------------------------------------------------------

    @Test
    void getLastTimestamp_returnsTimestampOfLastRow() {
        repo.append(List.of(
            makeRecord("2026-04-22 08:00", "15.0", "65"),
            makeRecord("2026-04-22 09:00", "16.0", "63"),
            makeRecord("2026-04-22 10:00", "17.5", "60")
        ), HEADERS);

        assertEquals("2026-04-22 10:00", repo.getLastTimestamp(),
                "Should return the timestamp from the last row");
    }

    // -------------------------------------------------------------------------
    // 6 — getFirstTimestamp() reads the first data row (not the header)
    // -------------------------------------------------------------------------

    @Test
    void getFirstTimestamp_returnsTimestampOfFirstDataRow() {
        repo.append(List.of(
            makeRecord("2026-04-22 08:00", "15.0", "65"),
            makeRecord("2026-04-22 09:00", "16.0", "63")
        ), HEADERS);

        assertEquals("2026-04-22 08:00", repo.getFirstTimestamp(),
                "Should return the timestamp from the first data row, not the header");
    }

    // -------------------------------------------------------------------------
    // 7 — append() with empty list writes nothing beyond header
    // -------------------------------------------------------------------------

    @Test
    void append_withEmptyList_writesOnlyHeader() throws Exception {
        repo.append(List.of(), HEADERS);

        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE));
        assertEquals(1, lines.size(), "Only the header row should exist after appending nothing");
    }

    // -------------------------------------------------------------------------
    // 8 — Timestamps survive quote-wrapped CSV fields
    // -------------------------------------------------------------------------

    @Test
    void getLastTimestamp_parsesCorrectly_withQuotedFields() {
        // append() wraps all values in quotes — verify the parser strips them
        repo.append(List.of(makeRecord("2026-04-22 12:00", "18.0", "58")), HEADERS);
        assertEquals("2026-04-22 12:00", repo.getLastTimestamp(),
                "Timestamp parser should strip surrounding quotes from CSV fields");
    }

    // -------------------------------------------------------------------------
    // Helper — builds a WeatherRecord matching HEADERS layout
    // -------------------------------------------------------------------------

    private WeatherRecord makeRecord(String timestamp, String temp, String humidity) {
        WeatherRecord r = new WeatherRecord();
        r.setValue("STATION", "SJSU");
        r.setValue("DATE",    timestamp.substring(0, 10));
        r.setValue("TIMESTAMP", timestamp);
        r.setValue("TEMP",    temp);
        r.setValue("HUMIDITY", humidity);
        return r;
    }
}
