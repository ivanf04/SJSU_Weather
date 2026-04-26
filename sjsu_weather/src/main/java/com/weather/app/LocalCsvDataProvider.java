package com.weather.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LocalCsvDataProvider
 *
 * Reads weather data from the local CSV backup file and supplies it to the UI.
 *
 * This class is the bridge between:
 * - stored CSV data on disk
 * - the JavaFX dashboard
 *
 * Expected CSV headers from the file:
 * - TIMESTAMP
 * - AirTF_Avg
 * - RH
 * - Rain_in_Tot
 * - SlrW_Avg
 * - WindSpeed_mph_avg
 *
 * Units in this file:
 * - temperature: Fahrenheit
 * - humidity: percent
 * - wind speed: mph
 * - solar: W/m^2
 * - rainfall: inches
 */
public class LocalCsvDataProvider implements DashboardDataProvider {

    /* ---------- CSV column names ---------- */

    /** Timestamp column */
    private static final String COL_TIMESTAMP = "TIMESTAMP";

    /** Air temperature in Fahrenheit */
    private static final String COL_TEMPERATURE = "AirTF_Avg";

    /** Relative humidity percentage */
    private static final String COL_HUMIDITY = "RH";

    /** Average wind speed in mph */
    private static final String COL_WIND_SPEED = "WindSpeed_mph_avg";

    /** Average solar radiation */
    private static final String COL_SOLAR = "SlrW_Avg";

    /** Total rainfall in inches */
    private static final String COL_RAINFALL = "Rain_in_Tot";

    /**
     * Timestamp format used by the CSV.
     */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * If the latest record is older than this, mark it as stale.
     */
    private static final int STALE_THRESHOLD_HOURS = 2;

    /* ---------- Fields ---------- */

    /** Path to the local CSV file */
    private final String csvFile;

    /** Forecast subsystem entry point */
    private final ArchiveClass archiveClass;

    /**
     * Constructor
     *
     * @param csvFile path to the local CSV file
     */
    public LocalCsvDataProvider(String csvFile) {
        this.csvFile = csvFile;
        this.archiveClass = new ArchiveClass();
    }

    /**
     * Returns the latest weather reading for the current weather section.
     */
    @Override
    public WeatherData getCurrentWeather() {
        List<WeatherData> all = parseAllRecords();

        if (all.isEmpty()) {
            return null;
        }

        WeatherData latest = all.get(all.size() - 1);

        // If the newest data is too old, mark it stale
        if (latest.getTimestamp() != null &&
                latest.getTimestamp().isBefore(LocalDateTime.now().minusHours(STALE_THRESHOLD_HOURS))) {
            return rebuildWithStatus(latest, WeatherDashboard.SystemStatus.STALE);
        }

        return rebuildWithStatus(latest, WeatherDashboard.SystemStatus.LIVE);
    }

    /**
     * Returns weather records between the selected start and end dates.
     */
    @Override
    public List<WeatherData> getHistoricalWeather(LocalDate startDate, LocalDate endDate) {
        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> {
                    LocalDate date = w.getTimestamp().toLocalDate();
                    return !date.isBefore(startDate) && !date.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns today's readings for the daily trend chart.
     */
    @Override
    public List<WeatherData> getDailyTrend() {
        LocalDate today = LocalDate.now();

        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> w.getTimestamp().toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    /**
     * Returns the last 7 days of readings for the weekly trend chart.
     */
    @Override
    public List<WeatherData> getWeeklyTrend() {
        LocalDate weekStart = LocalDate.now().minusDays(6);

        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> !w.getTimestamp().toLocalDate().isBefore(weekStart))
                .collect(Collectors.toList());
    }

    /**
     * Computes today's high and low temperature for the dashboard summary.
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
     * Returns forecast data using the forecast subsystem.
     *
     * Flow:
     * historical WeatherData -> ArchiveClass -> cache or PredictionEngine
     */
    @Override
    public List<ForecastEntry> getForecast() {
        List<WeatherData> historical = parseAllRecords();
        return archiveClass.getForecast(historical);
    }

    /**
     * Reads every row in the CSV file and converts it into WeatherData objects.
     */
    private List<WeatherData> parseAllRecords() {
        List<WeatherData> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {

            // Read header row first
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return results;
            }

            String[] headers = headerLine.replace("\"", "").split(",");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.replace("\"", "").split(",");

                // Convert one CSV row into a WeatherRecord
                WeatherRecord record = new WeatherRecord();
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    String value = i < values.length ? values[i].trim() : "";
                    record.setValue(header, value);
                }

                // Convert raw record into typed WeatherData
                WeatherData data = toWeatherData(record);
                if (data != null) {
                    results.add(data);
                }
            }

        } catch (IOException e) {
            System.err.println("CSV read error: " + csvFile + " (" + e.getMessage() + ")");
        }

        return results;
    }

    /**
     * Converts one raw WeatherRecord into one typed WeatherData object.
     */
    private WeatherData toWeatherData(WeatherRecord record) {

        String timestampText = record.getValue(COL_TIMESTAMP);
        if (timestampText == null || timestampText.isBlank()) {
            return null;
        }

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(timestampText, TIMESTAMP_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }

        // Read numeric values directly from the real CSV columns
        double temperature = parseDouble(record.getValue(COL_TEMPERATURE)); // °F
        double humidity = parseDouble(record.getValue(COL_HUMIDITY));       // %
        double windSpeed = parseDouble(record.getValue(COL_WIND_SPEED));    // mph
        double solar = parseDouble(record.getValue(COL_SOLAR));             // W/m^2
        double rainfall = parseDouble(record.getValue(COL_RAINFALL));       // in

        // Compute feels-like temperature in Fahrenheit
        double feelsLike = computeFeelsLikeFahrenheit(temperature, humidity, windSpeed);

        return new WeatherData(
                temperature,
                feelsLike,
                humidity,
                windSpeed,
                solar,
                rainfall,
                timestamp,
                true,
                WeatherDashboard.SystemStatus.CACHED
        );
    }

    /**
     * Safely parses a number from text.
     * Returns 0.0 if parsing fails.
     */
    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Computes "feels like" temperature in Fahrenheit.
     *
     * Rules:
     * - Use heat index when hot and humid
     * - Use wind chill when cold and windy
     * - Otherwise use actual temperature
     */
    private double computeFeelsLikeFahrenheit(double tempF, double humidity, double windMph) {

        // Heat index for hot/humid weather
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

        // Wind chill for cold/windy weather
        if (tempF <= 50 && windMph > 3) {
            return 35.74
                    + 0.6215 * tempF
                    - 35.75 * Math.pow(windMph, 0.16)
                    + 0.4275 * tempF * Math.pow(windMph, 0.16);
        }

        return tempF;
    }

    /**
     * Creates a copy of a WeatherData object with a different status.
     */
    private WeatherData rebuildWithStatus(WeatherData source, WeatherDashboard.SystemStatus status) {
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