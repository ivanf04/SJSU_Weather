package com.weather.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataServiceTest {

    private static final class FakeSource implements WeatherSource {
        private final List<WeatherRecord> rows;
        private final String[] headers;

        FakeSource(List<WeatherRecord> rows, String[] headers) {
            this.rows = rows;
            this.headers = headers;
        }

        @Override
        public List<WeatherRecord> fetchAll(String lastTimestamp) {
            return rows;
        }

        @Override
        public String[] getHeaders() {
            return headers;
        }
    }

    @Test
    void runSync_createsHeaderOnlyCsv_whenNoRowsButHeadersKnown_andFileMissing() throws Exception {
        Path temp = Files.createTempFile("weather_svc_", ".csv");
        Files.deleteIfExists(temp);

        LocalCsvRepository repo = new LocalCsvRepository(temp.toString());
        WeatherDataService svc = new WeatherDataService(
                new FakeSource(List.of(), new String[] { "TIMESTAMP", "AirTF_Avg" }),
                repo);

        svc.runSync();

        assertTrue(repo.dataFileExists());
        assertEquals(1, Files.readAllLines(temp).size());

        Files.deleteIfExists(temp);
    }
}
