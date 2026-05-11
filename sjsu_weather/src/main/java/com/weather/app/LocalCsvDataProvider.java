package com.weather.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads weather data from a local CSV file and provides it to the dashboard.
 *
 * This class connects the stored CSV data to the UI/application layer.
 *
 * Main responsibilities:
 * - read the local CSV backup file
 * - convert raw CSV rows into typed WeatherData objects
 * - filter data by date range
 * - provide daily and weekly trend data
 * - compute daily high/low summary
 * - delegate forecast generation/caching to ArchiveClass
 *
 * Uses CsvWeatherSchema so it is not tied to a specific CSV format.
 */
public class LocalCsvDataProvider implements DashboardDataProvider {

    /**
     * Timestamp format with seconds, used by most SJSU CSV rows.
     */
    private static final DateTimeFormatter TS_SPACE_SEC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Timestamp format without seconds.
     *
     * Kept as a fallback so slightly different timestamp formats do not break parsing.
     */
    private static final DateTimeFormatter TS_SPACE_MIN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * If the newest record is older than this many hours,
     * current weather is marked STALE instead of LIVE.
     */
    private static final int STALE_THRESHOLD_HOURS = 2;

    /**
     * Path to the local CSV file.
     */
    private final String csvFile;

    /**
     * Defines which CSV columns map to timestamp, temperature, humidity, etc.
     */
    private final CsvWeatherSchema schema;

    /**
     * Forecast facade used to retrieve cached or newly generated forecasts.
     */
    private final ArchiveClass archiveClass;

    /**
     * Cache parsed CSV rows so we don't reread file multiple times per refresh.
     *
     * This improves performance because several dashboard sections may request
     * data during the same UI refresh.
     */
    private List<WeatherData> cachedRecords;

    /**
     * Creates a data provider backed by a local CSV file.
     *
     * @param csvFile path to local CSV file
     * @param schema CSV column mapping
     * @param forecastArchive forecast facade/cache subsystem
     */
    public LocalCsvDataProvider(String csvFile,
                                CsvWeatherSchema schema,
                                ArchiveClass forecastArchive) {
        this.csvFile = csvFile;
        this.schema = schema;
        this.archiveClass = forecastArchive;
    }

    /**
     * Returns the latest weather reading for the dashboard cards.
     */
    @Override
    public WeatherData getCurrentWeather() {
        List<WeatherData> all = getAllRecords();

        if (all.isEmpty()) {
            return null;
        }

        WeatherData latest = all.get(all.size() - 1);

        // Mark data stale if it is too old compared with the system clock.
        if (latest.getTimestamp() != null &&
                latest.getTimestamp().isBefore(LocalDateTime.now().minusHours(STALE_THRESHOLD_HOURS))) {
            return rebuildWithStatus(latest, SystemStatus.STALE);
        }

        return rebuildWithStatus(latest, SystemStatus.LIVE);
    }

    /**
     * Returns all records between the selected start and end dates.
     *
     * Used by the historical table in the dashboard.
     */
    @Override
    public List<WeatherData> getHistoricalWeather(LocalDate startDate, LocalDate endDate) {
        return getAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> {
                    LocalDate date = w.getTimestamp().toLocalDate();
                    return !date.isBefore(startDate) && !date.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns weather readings for the latest available date in the dataset.
     *
     * This is better than using LocalDate.now() because the CSV may contain
     * historical/offline data that does not match the current system date.
     */
    @Override
    public List<WeatherData> getDailyTrend() {
        LocalDate latest = getLatestDate();

        return getAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> w.getTimestamp().toLocalDate().equals(latest))
                .collect(Collectors.toList());
    }

    /**
     * Returns weather readings for the 7-day window ending on the latest dataset date.
     */
    @Override
    public List<WeatherData> getWeeklyTrend() {
        LocalDate latest = getLatestDate();
        LocalDate weekStart = latest.minusDays(6);

        return getAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> {
                    LocalDate d = w.getTimestamp().toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(latest);
                })
                .collect(Collectors.toList());
    }

    /**
     * Computes high and low temperature for the latest daily trend.
     */
    @Override
    public DailySummary getDailySummary() {
        List<WeatherData> today = getDailyTrend();

        if (today.isEmpty()) {
            return new DailySummary(0, 0);
        }

        double high = today.stream()
                .mapToDouble(WeatherData::getTemperature)
                .max()
                .orElse(0);

        double low = today.stream()
                .mapToDouble(WeatherData::getTemperature)
                .min()
                .orElse(0);

        return new DailySummary(high, low);
    }

    /**
     * Retrieves forecast entries using the forecast/archive subsystem.
     */
    @Override
    public List<ForecastEntry> getForecast() {
        return archiveClass.getForecast(getAllRecords());
    }

    /**
     * Allows manual refresh if CSV updates.
     *
     * Clearing cachedRecords forces the next request to reread the CSV.
     */
    public void refreshCache() {
        cachedRecords = null;
    }

    /**
     * Returns cached records or loads them if needed.
     */
    private List<WeatherData> getAllRecords() {
        if (cachedRecords == null) {
            cachedRecords = parseAllRecords();
        }
        return cachedRecords;
    }

    /**
     * Get latest date from dataset instead of system clock.
     *
     * This makes charts work correctly even when showing historical datasets.
     */
    private LocalDate getLatestDate() {
        List<WeatherData> all = getAllRecords();
        if (all.isEmpty()) return LocalDate.now();

        return all.get(all.size() - 1).getTimestamp().toLocalDate();
    }

    /**
     * Reads the CSV file and converts each row into WeatherData.
     */
    private List<WeatherData> parseAllRecords() {
        List<WeatherData> result = new ArrayList<>();

        // If CSV does not exist yet, return empty data instead of crashing.
        if (!Files.exists(Paths.get(csvFile))) {
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {

            // First line contains column names.
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }

            // Remove UTF-8 BOM if present at beginning of file.
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = headerLine.replace("\"", "").split(",");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] cols = line.replace("\"", "").split(",");

                WeatherRecord record = new WeatherRecord();

                // Convert CSV row into key-value WeatherRecord.
                for (int i = 0; i < headers.length; i++) {
                    record.setValue(headers[i].trim(),
                            i < cols.length ? cols[i].trim() : "");
                }

                // Convert raw record into typed application model.
                WeatherData data = toWeatherData(record);
                if (data != null) {
                    result.add(data);
                }
            }

        } catch (IOException e) {
            System.err.println("CSV read error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Converts one raw WeatherRecord into one structured WeatherData object.
     */
    private WeatherData toWeatherData(WeatherRecord record) {

        LocalDateTime timestamp = parseTimestamp(
                record.getValue(schema.getTimestampColumn()));

        if (timestamp == null) return null;

        // Read numeric values using schema instead of hardcoded column names.
        double temperature = parseDouble(record.getValue(schema.getTemperatureColumn()));
        double humidity = parseDouble(record.getValue(schema.getHumidityColumn()));
        double wind = parseDouble(record.getValue(schema.getWindSpeedColumn()));
        double solar = parseDouble(record.getValue(schema.getSolarColumn()));
        double rainfall = parseDouble(record.getValue(schema.getRainfallColumn()));

        double feelsLike = computeFeelsLikeFahrenheit(temperature, humidity, wind);

        return new WeatherData(
                temperature,
                feelsLike,
                humidity,
                wind,
                solar,
                rainfall,
                timestamp,
                true,
                SystemStatus.CACHED
        );
    }

    /**
     * Parses timestamps using several supported formats.
     *
     * Returns null if timestamp cannot be parsed.
     */
    private static LocalDateTime parseTimestamp(String tsStr) {
        if (tsStr == null) return null;

        String t = tsStr.trim();
        if (t.isEmpty()) return null;

        // Try timestamp formats used by CSV files.
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{TS_SPACE_SEC, TS_SPACE_MIN}) {
            try {
                return LocalDateTime.parse(t, fmt);
            } catch (DateTimeParseException ignored) {}
        }

        // Try ISO local date-time as fallback.
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}

        // Try date-only format as final fallback.
        try {
            return LocalDate.parse(t).atStartOfDay();
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    /**
     * Computes "feels like" temperature in Fahrenheit.
     *
     * Uses:
     * - heat index when hot/humid
     * - wind chill when cold/windy
     * - actual temperature otherwise
     */
    private double computeFeelsLikeFahrenheit(double tempF, double humidity, double windMph) {

        // Heat index formula for hot and humid conditions.
        if (tempF >= 80 && humidity >= 40) {
            return -42.379
                    + 2.04901523 * tempF
                    + 10.14333127 * humidity
                    - 0.22475541 * tempF * humidity
                    - 0.00683783 * tempF * tempF
                    - 0.05481717 * humidity * humidity
                    + 0.00122874 * tempF * tempF * humidity
                    + 0.00085282 * tempF * humidity * humidity
                    - 0.00000199 * tempF * tempF * humidity * humidity;
        }

        // Wind chill formula for cold and windy conditions.
        if (tempF <= 50 && windMph > 3) {
            return 35.74
                    + 0.6215 * tempF
                    - 35.75 * Math.pow(windMph, 0.16)
                    + 0.4275 * tempF * Math.pow(windMph, 0.16);
        }

        return tempF;
    }

    /**
     * Safely parses numeric CSV values.
     *
     * Returns 0.0 if parsing fails.
     */
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Creates a copy of WeatherData with a new SystemStatus.
     *
     * Used so the same weather reading can be labeled LIVE or STALE
     * without mutating the original immutable object.
     */
    private WeatherData rebuildWithStatus(WeatherData source, SystemStatus status) {
        return new WeatherData(
                source.getTemperature(),
                source.getFeelsLike(),
                source.getHumidity(),
                source.getWindSpeed(),
                source.getSolarIrradiance(),
                source.getRainfall(),
                source.getTimestamp(),
                source.isCached(),
                status
        );
    }
}