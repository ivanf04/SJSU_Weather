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
 * The CSV column names are supplied by CsvWeatherSchema, so this class is not
 * locked to one exact CSV format.
 */
public class LocalCsvDataProvider implements DashboardDataProvider {

    private static final DateTimeFormatter TS_SPACE_SEC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TS_SPACE_MIN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int STALE_THRESHOLD_HOURS = 2;

    private final String csvFile;
    private final CsvWeatherSchema schema;
    private final ArchiveClass archiveClass;

    private List<WeatherData> cachedRecords;

    public LocalCsvDataProvider(String csvFile,
                                CsvWeatherSchema schema,
                                ArchiveClass forecastArchive) {
        this.csvFile = csvFile;
        this.schema = schema;
        this.archiveClass = forecastArchive;
    }

    @Override
    public WeatherData getCurrentWeather() {
        List<WeatherData> all = getAllRecords();

        if (all.isEmpty()) {
            return null;
        }

        WeatherData latest = all.get(all.size() - 1);

        if (latest.getTimestamp() != null
                && latest.getTimestamp().isBefore(LocalDateTime.now().minusHours(STALE_THRESHOLD_HOURS))) {
            return rebuildWithStatus(latest, SystemStatus.STALE);
        }

        return rebuildWithStatus(latest, SystemStatus.LIVE);
    }

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

    @Override
    public List<WeatherData> getDailyTrend() {
        LocalDate today = LocalDate.now();

        return getAllRecords().stream()
                .filter(w -> w.getTimestamp() != null)
                .filter(w -> w.getTimestamp().toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    @Override
    public List<WeatherData> getWeeklyTrend() {
        LocalDate weekStart = LocalDate.now().minusDays(6);

        return getAllRecords().stream()
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

    @Override
    public List<ForecastEntry> getForecast() {
        return archiveClass.getForecast(getAllRecords());
    }

    /**
     * Clears cached parsed records.
     *
     * Call this when the CSV changes and the provider should reread it.
     */
    public void refreshCache() {
        cachedRecords = null;
    }

    private List<WeatherData> getAllRecords() {
        if (cachedRecords == null) {
            cachedRecords = parseAllRecords();
        }

        return cachedRecords;
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
        LocalDateTime timestamp = parseTimestamp(record.getValue(schema.getTimestampColumn()));

        if (timestamp == null) {
            return null;
        }

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

    private static LocalDateTime parseTimestamp(String tsStr) {
        if (tsStr == null) {
            return null;
        }

        String text = tsStr.trim();

        if (text.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : new DateTimeFormatter[] { TS_SPACE_SEC, TS_SPACE_MIN }) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }

        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try date-only format.
        }

        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private double computeFeelsLikeFahrenheit(double tempF, double humidity, double windMph) {
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

        if (tempF <= 50 && windMph > 3) {
            return 35.74
                    + 0.6215 * tempF
                    - 35.75 * Math.pow(windMph, 0.16)
                    + 0.4275 * tempF * Math.pow(windMph, 0.16);
        }

        return tempF;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

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