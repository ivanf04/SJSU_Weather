package com.weather.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Typed weather model used by the UI and application layer.
 *
 * This class represents one processed weather reading.
 * It is different from WeatherRecord:
 * - WeatherRecord = raw key/value row from CSV or scraper
 * - WeatherData   = structured object with typed fields
 *
 * Current units used by LocalCsvDataProvider:
 * - temperature / feelsLike: Celsius
 * - humidity: percent
 * - windSpeed: knots
 * - solarIrradiance: W/m^2
 * - rainfall: mm
 */
public class WeatherData {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ---------- Core weather fields ---------- */

    private final double temperature;
    private final double feelsLike;
    private final double humidity;
    private final double windSpeed;
    private final double solarIrradiance;
    private final double rainfall;

    /* ---------- Metadata ---------- */

    private final LocalDateTime timestamp;
    private final boolean cached;
    private final WeatherDashboard.SystemStatus status;

    /**
     * Creates one structured weather reading.
     */
    public WeatherData(double temperature,
                       double feelsLike,
                       double humidity,
                       double windSpeed,
                       double solarIrradiance,
                       double rainfall,
                       LocalDateTime timestamp,
                       boolean cached,
                       WeatherDashboard.SystemStatus status) {
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.solarIrradiance = solarIrradiance;
        this.rainfall = rainfall;
        this.timestamp = timestamp;
        this.cached = cached;
        this.status = status;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getSolarIrradiance() {
        return solarIrradiance;
    }

    public double getRainfall() {
        return rainfall;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isCached() {
        return cached;
    }

    public WeatherDashboard.SystemStatus getStatus() {
        return status;
    }

    /**
     * Returns a formatted timestamp string for the UI.
     */
    public String getFormattedTimestamp() {
        return timestamp == null ? "--" : timestamp.format(DISPLAY_FORMAT);
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "temperature=" + temperature +
                ", feelsLike=" + feelsLike +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", solarIrradiance=" + solarIrradiance +
                ", rainfall=" + rainfall +
                ", timestamp=" + timestamp +
                ", cached=" + cached +
                ", status=" + status +
                '}';
    }
}