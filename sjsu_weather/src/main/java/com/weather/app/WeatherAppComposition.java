package com.weather.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Composition root of the application.
 *
 * This class is responsible for constructing and wiring together
 * all major components of the system.
 *
 * It centralizes decisions about:
 * - which weather source to use
 * - where the CSV file is stored
 * - which forecast model is used
 * - how caching is configured
 *
 * This makes the system:
 * - easier to modify
 * - easier to test
 * - easier to extend
 *
 * Instead of each class creating its own dependencies,
 * everything is configured here.
 */
public final class WeatherAppComposition {

    /** Default SJSU weather station URL */
    public static final String DEFAULT_SJSU_ROOF_URL =
            "https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/";

    /** Default filename for local CSV backup */
    public static final String DEFAULT_BACKUP_FILENAME = "sjsu_weather_backup.csv";

    /** Default filename for forecast cache */
    public static final String FORECAST_CACHE_FILENAME = "forecast_cache.json";

    /** Path to CSV file used for storage */
    private final Path csvPath;

    /** Weather source (e.g., SJSU fetcher) */
    private final WeatherSource weatherSource;

    /** Schema describing how CSV columns map to data fields */
    private final CsvWeatherSchema csvSchema;

    /** Facade for forecast subsystem (model + cache) */
    private final ArchiveClass forecastArchive;

    /**
     * Constructor used internally by createDefault().
     */
    public WeatherAppComposition(Path csvPath,
                                 WeatherSource weatherSource,
                                 CsvWeatherSchema csvSchema,
                                 ArchiveClass forecastArchive) {
        this.csvPath = csvPath;
        this.weatherSource = weatherSource;
        this.csvSchema = csvSchema;
        this.forecastArchive = forecastArchive;
    }

    /**
     * Returns the CSV path used by the application.
     */
    public Path getCsvPath() {
        return csvPath;
    }

    /**
     * Returns the forecast facade.
     */
    public ArchiveClass getForecastArchive() {
        return forecastArchive;
    }

    /**
     * Determines default CSV location based on working directory.
     */
    public static Path defaultCsvPath() {
        return Paths.get(System.getProperty("user.dir"), DEFAULT_BACKUP_FILENAME)
                .normalize()
                .toAbsolutePath();
    }

    /**
     * Determines where forecast cache file should live (same folder as CSV).
     */
    static Path forecastCachePathBesideCsv(Path csvPath) {
        Path parent = csvPath.getParent();

        if (parent == null) {
            return Paths.get(FORECAST_CACHE_FILENAME).toAbsolutePath();
        }

        return parent.resolve(FORECAST_CACHE_FILENAME);
    }

    /**
     * Creates the default application configuration.
     *
     * This method wires together:
     * - Weather source (SjsuWeatherFetcher)
     * - Forecast model (PredictionEngine)
     * - Cache (ForecastCache)
     * - Facade (ArchiveClass)
     * - Schema (CsvWeatherSchema)
     */
    public static WeatherAppComposition createDefault() {
        Path csv = defaultCsvPath();

        // Create forecast cache (JSON file)
        ForecastCache cache = new ForecastCache(
                forecastCachePathBesideCsv(csv).toString()
        );

        // Choose forecast algorithm (Strategy pattern)
        ForecastModel forecastModel = new PredictionEngine();

        // Create facade combining model + cache
        ArchiveClass archive = new ArchiveClass(forecastModel, cache);

        // Build full composition
        return new WeatherAppComposition(
                csv,
                new SjsuWeatherFetcher(DEFAULT_SJSU_ROOF_URL),
                CsvWeatherSchema.sjsuRoofSchema(),
                archive
        );
    }

    /**
     * Ensures the directory for the CSV file exists.
     */
    public void ensureCsvParentDirectoriesExist() {
        try {
            Path parent = csvPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println(
                "Could not create parent directory for CSV: " + e.getMessage()
            );
        }
    }

    /**
     * Creates the sync service that pulls data from source into repository.
     */
    public WeatherDataService createSyncService() {
        return new WeatherDataService(
                weatherSource,
                new LocalCsvRepository(csvPath.toString())
        );
    }

    /**
     * Runs data synchronization.
     */
    public void runSync() {
        createSyncService().runSync();
    }

    /**
     * Creates the data provider used by the UI.
     *
     * This connects:
     * - CSV data
     * - schema mapping
     * - forecast subsystem
     */
    public DashboardDataProvider createDashboardDataProvider() {
        return new LocalCsvDataProvider(
                csvPath.toString(),
                csvSchema,
                forecastArchive
        );
    }
}