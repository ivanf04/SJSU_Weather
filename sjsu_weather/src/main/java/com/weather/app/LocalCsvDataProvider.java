package com.weather.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads weather data from the local CSV file and provides it to the dashboard.
 *
 * Expected CSV column names (from SJSU DHRoof station):
 *   TIMESTAMP, TMPC, RELH, SKNT, SOLR, RAIN
 * Update the COL_* constants if the station uses different header names.
 */
public class LocalCsvDataProvider implements DashboardDataProvider {

    // --- CSV column name constants ---
    private static final String COL_TIMESTAMP   = "TIMESTAMP";
    private static final String COL_TEMPERATURE = "TMPC";   // °C
    private static final String COL_HUMIDITY    = "RELH";   // %
    private static final String COL_WIND_SPEED  = "SKNT";   // knots
    private static final String COL_SOLAR       = "SOLR";   // W/m²
    private static final String COL_RAINFALL    = "RAIN";   // mm

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Data is considered stale after this many hours with no update. */
    private static final int STALE_THRESHOLD_HOURS = 2;

    private final String csvFile;

    public LocalCsvDataProvider(String csvFile) {
        this.csvFile = csvFile;
    }

    // ---------- DashboardDataProvider ----------

    @Override
    public WeatherData getCurrentWeather() {
        List<WeatherData> all = parseAllRecords();
        if (all.isEmpty()) return null;

        WeatherData latest = all.get(all.size() - 1);

        if (latest.getTimestamp() != null &&
                latest.getTimestamp().isBefore(LocalDateTime.now().minusHours(STALE_THRESHOLD_HOURS))) {
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
        if (today.isEmpty()) return new DailySummary(0, 0);
        double high = today.stream().mapToDouble(WeatherData::getTemperature).max().orElse(0);
        double low  = today.stream().mapToDouble(WeatherData::getTemperature).min().orElse(0);
        return new DailySummary(high, low);
    }

    /**
     * Produces a simple 3-day forecast by computing the average temperature
     * over the past week and applying a linear trend.
     */
    @Override
    public List<ForecastEntry> getForecast() {
        List<WeatherData> weekly = getWeeklyTrend();
        List<ForecastEntry> forecast = new ArrayList<>();
        if (weekly.isEmpty()) return forecast;

        double avgTemp = weekly.stream()
                .mapToDouble(WeatherData::getTemperature)
                .average()
                .orElse(0);

        List<Double> dailyAvgs = computeDailyAverages(weekly);
        double trend = dailyAvgs.size() >= 2
                ? (dailyAvgs.get(dailyAvgs.size() - 1) - dailyAvgs.get(0)) / dailyAvgs.size()
                : 0;

        String[] confidenceLabels = {"High", "Medium", "Low"};
        for (int i = 1; i <= 3; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            double predicted = avgTemp + trend * i;
            forecast.add(new ForecastEntry(date, predicted, confidenceLabels[i - 1]));
        }
        return forecast;
    }

    // ---------- CSV parsing ----------

    private List<WeatherData> parseAllRecords() {
        List<WeatherData> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return result;

            String[] headers = headerLine.replace("\"", "").split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.replace("\"", "").split(",");

                WeatherRecord record = new WeatherRecord();
                for (int i = 0; i < headers.length; i++) {
                    record.setValue(headers[i].trim(), i < cols.length ? cols[i].trim() : "");
                }

                WeatherData data = toWeatherData(record);
                if (data != null) result.add(data);
            }
        } catch (IOException e) {
            System.err.println("CSV read error: " + e.getMessage());
        }
        return result;
    }

    private WeatherData toWeatherData(WeatherRecord record) {
        String tsStr = record.getValue(COL_TIMESTAMP);
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(tsStr, TIMESTAMP_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }

        double tempC    = parseDouble(record.getValue(COL_TEMPERATURE));
        double humidity = parseDouble(record.getValue(COL_HUMIDITY));
        double wind     = parseDouble(record.getValue(COL_WIND_SPEED));
        double solar    = parseDouble(record.getValue(COL_SOLAR));
        double rainfall = parseDouble(record.getValue(COL_RAINFALL));
        double feelsLike = computeFeelsLike(tempC, humidity, wind);

        return new WeatherData(tempC, feelsLike, humidity, wind, solar, rainfall,
                timestamp, true, WeatherDashboard.SystemStatus.CACHED);
    }

    // ---------- "Feels like" calculation ----------

    /**
     * Returns feels-like temperature in °C.
     * Uses NOAA Heat Index when hot and humid, Wind Chill when cold and windy.
     */
    private double computeFeelsLike(double tempC, double humidity, double windKnots) {
        double tempF   = celsiusToFahrenheit(tempC);
        double windMph = windKnots * 1.15078;

        if (tempF >= 80 && humidity >= 40) {
            // NOAA Rothfusz Heat Index equation (°F)
            double hi = -42.379
                    + 2.04901523  * tempF
                    + 10.14333127 * humidity
                    - 0.22475541  * tempF * humidity
                    - 0.00683783  * tempF * tempF
                    - 0.05481717  * humidity * humidity
                    + 0.00122874  * tempF * tempF * humidity
                    + 0.00085282  * tempF * humidity * humidity
                    - 0.00000199  * tempF * tempF * humidity * humidity;
            return fahrenheitToCelsius(hi);
        }

        if (tempF <= 50 && windMph > 3) {
            // NWS Wind Chill equation (°F)
            double wc = 35.74
                    + 0.6215  * tempF
                    - 35.75   * Math.pow(windMph, 0.16)
                    + 0.4275  * tempF * Math.pow(windMph, 0.16);
            return fahrenheitToCelsius(wc);
        }

        return tempC;
    }

    // ---------- Forecast helpers ----------

    private List<Double> computeDailyAverages(List<WeatherData> data) {
        LocalDate start = data.stream()
                .map(w -> w.getTimestamp().toLocalDate())
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now().minusDays(6));

        List<Double> avgs = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            final LocalDate day = d;
            data.stream()
                .filter(w -> w.getTimestamp().toLocalDate().equals(day))
                .mapToDouble(WeatherData::getTemperature)
                .average()
                .ifPresent(avgs::add);
        }
        return avgs;
    }

    // ---------- Utility ----------

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private double celsiusToFahrenheit(double c) { return c * 9.0 / 5.0 + 32; }
    private double fahrenheitToCelsius(double f) { return (f - 32) * 5.0 / 9.0; }

    private WeatherData rebuildWithStatus(WeatherData src, WeatherDashboard.SystemStatus status) {
        return new WeatherData(
                src.getTemperature(), src.getFeelsLike(), src.getHumidity(),
                src.getWindSpeed(), src.getSolarIrradiance(), src.getRainfall(),
                src.getTimestamp(), src.isCached(), status);
    }
}
