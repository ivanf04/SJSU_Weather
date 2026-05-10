package com.weather.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Composition root: one place for CSV location, sync wiring, and forecast collaborators.
 */
public final class WeatherAppComposition {

    public static final String DEFAULT_SJSU_ROOF_URL =
            "https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/";
    public static final String DEFAULT_BACKUP_FILENAME = "sjsu_weather_backup.csv";
    public static final String FORECAST_CACHE_FILENAME = "forecast_cache.json";

    private final Path csvPath;
    private final WeatherSource weatherSource;
    private final ArchiveClass forecastArchive;

    public WeatherAppComposition(Path csvPath, WeatherSource weatherSource, ArchiveClass forecastArchive) {
        this.csvPath = csvPath;
        this.weatherSource = weatherSource;
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

    /**
     * Forecast JSON sits next to the backup CSV so cwd changes do not split cache from data.
     */
    static Path forecastCachePathBesideCsv(Path csvPath) {
        Path parent = csvPath.getParent();
        if (parent == null) {
            return Paths.get(FORECAST_CACHE_FILENAME).toAbsolutePath();
        }
        return parent.resolve(FORECAST_CACHE_FILENAME);
    }

    /**
     * Standard wiring: SJSU fetcher, 5-day {thru prediction engine},
     * disk cache beside the CSV file.
     */
    public static WeatherAppComposition createDefault() {
        Path csv = defaultCsvPath();
        ForecastCache cache = new ForecastCache(forecastCachePathBesideCsv(csv).toString());
        ArchiveClass archive = new ArchiveClass(new PredictionEngine(), cache);
        return new WeatherAppComposition(csv, new SjsuWeatherFetcher(DEFAULT_SJSU_ROOF_URL), archive);
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
        return new WeatherDataService(weatherSource, new LocalCsvRepository(csvPath.toString()));
    }

    public void runSync() {
        createSyncService().runSync();
    }

    public DashboardDataProvider createDashboardDataProvider() {
        return new LocalCsvDataProvider(csvPath.toString(), forecastArchive);
    }
}
