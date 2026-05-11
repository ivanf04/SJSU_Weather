package com.weather.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LocalCsvDataProviderTest {

    @Test
    void parsesDateOnlyTimestamp_rowsBecomeWeatherData() throws Exception {
        Path p = Files.createTempFile("roof_", ".csv");

        String csv = ""
                + "Station,SynopticID,TIMESTAMP,AirTF_Avg,RH,Rain_in_Tot,SlrW_Avg,WindSpeed_mph_avg,WindDir_avg,WindDir_StdDev,WindSpeed_mph_max\n"
                + "SJSU,123,2026-05-01,55.0,40.0,0.1,100.0,2.0,180,5,4.0\n";

        Files.writeString(p, csv);

        Path fc = Files.createTempFile("forecast_", ".json");

        ArchiveClass archive = new ArchiveClass(
                new PredictionEngine(),
                new ForecastCache(fc.toString())
        );

        LocalCsvDataProvider provider = new LocalCsvDataProvider(
                p.toString(),
                CsvWeatherSchema.sjsuRoofSchema(),
                archive
        );

        WeatherData current = provider.getCurrentWeather();

        assertNotNull(current);
        assertEquals(55.0, current.getTemperature(), 0.001);
        assertEquals(40.0, current.getHumidity(), 0.001);
        assertEquals(2.0, current.getWindSpeed(), 0.001);
        assertEquals(100.0, current.getSolarIrradiance(), 0.001);
        assertEquals(0.1, current.getRainfall(), 0.001);

        assertEquals(1, provider.getHistoricalWeather(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 1)
        ).size());

        Files.deleteIfExists(fc);
        Files.deleteIfExists(p);
    }

    @Test
    void parsesSpaceSeparatedDateTime() throws Exception {
        Path p = Files.createTempFile("roof_", ".csv");

        String csv = ""
                + "Station,SynopticID,TIMESTAMP,AirTF_Avg,RH,Rain_in_Tot,SlrW_Avg,WindSpeed_mph_avg,WindDir_avg,WindDir_StdDev,WindSpeed_mph_max\n"
                + "SJSU,123,2026-05-09 14:30:00,60.0,50.0,0.0,0.0,1.0,180,5,2.0\n";

        Files.writeString(p, csv);

        Path fc = Files.createTempFile("forecast_", ".json");

        ArchiveClass archive = new ArchiveClass(
                new PredictionEngine(),
                new ForecastCache(fc.toString())
        );

        LocalCsvDataProvider provider = new LocalCsvDataProvider(
                p.toString(),
                CsvWeatherSchema.sjsuRoofSchema(),
                archive
        );

        WeatherData current = provider.getCurrentWeather();

        assertNotNull(current);
        assertEquals(60.0, current.getTemperature(), 0.001);

        Files.deleteIfExists(fc);
        Files.deleteIfExists(p);
    }
}