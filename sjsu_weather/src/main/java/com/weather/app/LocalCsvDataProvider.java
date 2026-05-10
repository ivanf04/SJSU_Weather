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
 * Reads weather data from the local CSV file and provides it to the dashboard.
 *
 * Matches SJSU DHRoof CSV: TIMESTAMP (date or date-time), AirTF_Avg, RH,
 * WindSpeed_mph_avg, SlrW_Avg, Rain_in_Tot or RAIN.
 */
public class LocalCsvDataProvider implements DashboardDataProvider {

    private static final String COL_TIMESTAMP = "TIMESTAMP";
    private static final String COL_TEMPERATURE = "AirTF_Avg";
    private static final String COL_HUMIDITY = "RH";
    private static final String COL_WIND_SPEED = "WindSpeed_mph_avg";
    private static final String COL_SOLAR = "SlrW_Avg";
    private static final String COL_RAINFALL = "RAIN";
    /** SJSU roof export uses this name instead of RAIN. */
    private static final String COL_RAINFALL_ALT = "Rain_in_Tot";

    private static final DateTimeFormatter TS_SPACE_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TS_SPACE_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int STALE_THRESHOLD_HOURS = 2;

    private final String csvFile;
    private final ArchiveClass archiveClass;

    public LocalCsvDataProvider(String csvFile, ArchiveClass forecastArchive) {
        this.csvFile = csvFile;
        this.archiveClass = forecastArchive;
    }

    @Override
    public WeatherData getCurrentWeather() {
        List<WeatherData> all = parseAllRecords();
        if (all.isEmpty()) {
            return null;
        }

        WeatherData latest = all.get(all.size() - 1);

        if (latest.getTimestamp() != null
                && latest.getTimestamp().isBefore(LocalDateTime.now().minusHours(STALE_THRESHOLD_HOURS))) {
            return rebuildWithStatus(latest, WeatherDashboard.SystemStatus.STALE);
        }

        return rebuildWithStatus(latest, WeatherDashboard.SystemStatus.LIVE);
    }

    @Override
    public List<WeatherData> getHistoricalWeather(LocalDate startDate, LocalDate endDate) {
        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> {
                    LocalDate d = w.getTimestamp().toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WeatherData> getDailyTrend() {
        LocalDate today = LocalDate.now();
        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> w.getTimestamp().toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    @Override
    public List<WeatherData> getWeeklyTrend() {
        LocalDate weekStart = LocalDate.now().minusDays(6);
        return parseAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> !w.getTimestamp().toLocalDate().isBefore(weekStart))
                .collect(Collectors.toList());
    }

    @Override
    public DailySummary getDailySummary() {
        List<WeatherData> today = getDailyTrend();
        if (today.isEmpty()) {
            return new DailySummary(0, 0);
        }

        double high = today.stream().mapToDouble(WeatherData::getTemperature).max().orElse(0);
        double low = today.stream().mapToDouble(WeatherData::getTemperature).min().orElse(0);
        return new DailySummary(high, low);
    }

    /**
     * Uses the full forecast pipeline: ArchiveClass -> ForecastCache ->
     * PredictionEngine -> TemperatureAggregator + LinearTrendCalculator +
     * ConfidenceEvaluator
     */
    @Override
    public List<ForecastEntry> getForecast() {
        List<WeatherData> historical = parseAllRecords();
        return archiveClass.getForecast(historical);
    }

    private List<WeatherData> parseAllRecords() {
        List<WeatherData> result = new ArrayList<>();

        if (!Files.exists(Paths.get(csvFile))) {
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = headerLine.replace("\"", "").split(",");
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] cols = line.replace("\"", "").split(",");
                WeatherRecord record = new WeatherRecord();

                for (int i = 0; i < headers.length; i++) {
                    record.setValue(headers[i].trim(), i < cols.length ? cols[i].trim() : "");
                }

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

    private WeatherData toWeatherData(WeatherRecord record) {
        String tsStr = record.getValue(COL_TIMESTAMP);
        LocalDateTime timestamp = parseTimestamp(tsStr);
        if (timestamp == null) {
            return null;
        }

        double tempC = parseDouble(record.getValue(COL_TEMPERATURE));
        double humidity = parseDouble(record.getValue(COL_HUMIDITY));
        double wind = parseDouble(record.getValue(COL_WIND_SPEED));
        double solar = parseDouble(record.getValue(COL_SOLAR));
        double rainfall = parseDouble(record.getValue(COL_RAINFALL));
        if (rainfall == 0.0 && record.getValue(COL_RAINFALL).isBlank()) {
            rainfall = parseDouble(record.getValue(COL_RAINFALL_ALT));
        }
        double feelsLike = computeFeelsLike(tempC, humidity, wind);

        return new WeatherData(
                tempC,
                feelsLike,
                humidity,
                wind,
                solar,
                rainfall,
                timestamp,
                true,
                WeatherDashboard.SystemStatus.CACHED
        );
    }

    /**
     * SJSU CSV mixes {@code yyyy-MM-dd} rows with {@code yyyy-MM-dd HH:mm:ss} (and similar) rows.
     */
    private static LocalDateTime parseTimestamp(String tsStr) {
        if (tsStr == null) {
            return null;
        }
        String t = tsStr.trim();
        if (t.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter fmt : new DateTimeFormatter[] { TS_SPACE_SEC, TS_SPACE_MIN }) {
            try {
                return LocalDateTime.parse(t, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Returns feels-like temperature in °C.
     */
    private double computeFeelsLike(double tempF, double humidity, double windMph) {

        // Heat index only applies in hot conditions
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

        // Wind chill only applies in cold/windy conditions
        if (tempF <= 50 && windMph > 3) {
            return 35.74
                    + 0.6215 * tempF
                    - 35.75 * Math.pow(windMph, 0.16)
                    + 0.4275 * tempF * Math.pow(windMph, 0.16);
        }

        return tempF;
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double celsiusToFahrenheit(double c) {
        return c * 9.0 / 5.0 + 32;
    }

    private double fahrenheitToCelsius(double f) {
        return (f - 32) * 5.0 / 9.0;
    }

    private WeatherData rebuildWithStatus(WeatherData src, WeatherDashboard.SystemStatus status) {
        return new WeatherData(
                src.getTemperature(),
                src.getFeelsLike(),
                src.getHumidity(),
                src.getWindSpeed(),
                src.getSolarIrradiance(),
                src.getRainfall(),
                src.getTimestamp(),
                src.isCached(),
                status
        );
    }
}
