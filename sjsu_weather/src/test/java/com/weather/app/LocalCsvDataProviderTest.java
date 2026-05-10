package com.weather.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalCsvDataProviderTest {

    @Test
    void parsesDateOnlyTimestamp_rowsBecomeWeatherData() throws Exception {
        Path p = Files.createTempFile("roof_", ".csv");
        String csv = ""
                + "Station,TIMESTAMP,AirTF_Avg,RH,WindSpeed_mph_avg,SlrW_Avg,Rain_in_Tot\n"
                + "X,2026-05-01,55.0,40.0,2.0,100.0,0.1\n";
        Files.writeString(p, csv);

        Path fc = Files.createTempFile("forecast_", ".json");
        ArchiveClass archive = new ArchiveClass(new PredictionEngine(), new ForecastCache(fc.toString()));
        LocalCsvDataProvider provider = new LocalCsvDataProvider(p.toString(), archive);
        assertNotNull(provider.getCurrentWeather());
        assertEquals(1, provider.getHistoricalWeather(
                java.time.LocalDate.of(2026, 4, 1),
                java.time.LocalDate.of(2026, 6, 1)).size());

        Files.deleteIfExists(fc);
        Files.deleteIfExists(p);
    }

    @Test
    void parsesSpaceSeparatedDateTime() throws Exception {
        Path p = Files.createTempFile("roof_", ".csv");
        String csv = ""
                + "TIMESTAMP,AirTF_Avg,RH,WindSpeed_mph_avg,SlrW_Avg,RAIN\n"
                + "2026-05-09 14:30:00,60.0,50.0,1.0,0.0,0.0\n";
        Files.writeString(p, csv);

        Path fc = Files.createTempFile("forecast_", ".json");
        ArchiveClass archive = new ArchiveClass(new PredictionEngine(), new ForecastCache(fc.toString()));
        LocalCsvDataProvider provider = new LocalCsvDataProvider(p.toString(), archive);
        assertNotNull(provider.getCurrentWeather());

        Files.deleteIfExists(fc);
        Files.deleteIfExists(p);
    }
}
