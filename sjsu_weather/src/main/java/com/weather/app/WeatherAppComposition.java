package com.weather.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Composition root.
 *
 * This is the one place where the app chooses:
 * - CSV path
 * - weather source
 * - CSV schema
 * - forecast model
 * - forecast cache
 *
 * Keeping this wiring here makes the rest of the app easier to change.
 */
public final class WeatherAppComposition {

    public static final String DEFAULT_SJSU_ROOF_URL =
            "https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/";

    public static final String DEFAULT_BACKUP_FILENAME = "sjsu_weather_backup.csv";
    public static final String FORECAST_CACHE_FILENAME = "forecast_cache.json";

    private final Path csvPath;
    private final WeatherSource weatherSource;
    private final CsvWeatherSchema csvSchema;
    private final ArchiveClass forecastArchive;

    public WeatherAppComposition(Path csvPath,
                                 WeatherSource weatherSource,
                                 CsvWeatherSchema csvSchema,
                                 ArchiveClass forecastArchive) {
        this.csvPath = csvPath;
        this.weatherSource = weatherSource;
        this.csvSchema = csvSchema;
        this.forecastArchive = forecastArchive;
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public ArchiveClass getForecastArchive() {
        return forecastArchive;
    }

    public static Path defaultCsvPath() {
        return Paths.get(System.getProperty("user.dir"), DEFAULT_BACKUP_FILENAME)
                .normalize()
                .toAbsolutePath();
    }

    static Path forecastCachePathBesideCsv(Path csvPath) {
        Path parent = csvPath.getParent();

        if (parent == null) {
            return Paths.get(FORECAST_CACHE_FILENAME).toAbsolutePath();
        }

        return parent.resolve(FORECAST_CACHE_FILENAME);
    }

    public static WeatherAppComposition createDefault() {
        Path csv = defaultCsvPath();

        ForecastCache cache = new ForecastCache(forecastCachePathBesideCsv(csv).toString());
        ForecastModel forecastModel = new PredictionEngine();
        ArchiveClass archive = new ArchiveClass(forecastModel, cache);

        return new WeatherAppComposition(
                csv,
                new SjsuWeatherFetcher(DEFAULT_SJSU_ROOF_URL),
                CsvWeatherSchema.sjsuRoofSchema(),
                archive
        );
    }

    public void ensureCsvParentDirectoriesExist() {
        try {
            Path parent = csvPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Could not create parent directory for CSV: " + e.getMessage());
        }
    }

    public WeatherDataService createSyncService() {
        return new WeatherDataService(
                weatherSource,
                new LocalCsvRepository(csvPath.toString())
        );
    }

    public void runSync() {
        createSyncService().runSync();
    }

    public DashboardDataProvider createDashboardDataProvider() {
        return new LocalCsvDataProvider(
                csvPath.toString(),
                csvSchema,
                forecastArchive
        );
    }
}