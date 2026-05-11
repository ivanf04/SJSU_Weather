package com.weather.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Typed weather model used by the app and UI.
 *
 * WeatherRecord = raw key/value CSV row.
 * WeatherData = structured weather object used by dashboard and forecast logic.
 *
 * Current units:
 * - temperature / feelsLike: Fahrenheit
 * - humidity: percent
 * - windSpeed: mph
 * - solarIrradiance: W/m^2
 * - rainfall: inches
 */
public class WeatherData {

    /**
     * Format used when displaying timestamp in UI.
     */
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Air temperature in Fahrenheit.
     */
    private final double temperature;

    /**
     * Feels-like temperature in Fahrenheit.
     */
    private final double feelsLike;

    /**
     * Relative humidity percentage.
     */
    private final double humidity;

    /**
     * Wind speed in miles per hour.
     */
    private final double windSpeed;

    /**
     * Solar irradiance in W/m^2.
     */
    private final double solarIrradiance;

    /**
     * Rainfall in inches.
     */
    private final double rainfall;

    /**
     * Timestamp for this weather reading.
     */
    private final LocalDateTime timestamp;

    /**
     * Whether this record came from cached/local data.
     */
    private final boolean cached;

    /**
     * Status of the data, such as LIVE, CACHED, STALE, ERROR, or LOADING.
     */
    private final SystemStatus status;

    /**
     * Creates one structured weather data point.
     */
    public WeatherData(double temperature,
                       double feelsLike,
                       double humidity,
                       double windSpeed,
                       double solarIrradiance,
                       double rainfall,
                       LocalDateTime timestamp,
                       boolean cached,
                       SystemStatus status) {
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

    /**
     * Returns temperature in Fahrenheit.
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns feels-like temperature in Fahrenheit.
     */
    public double getFeelsLike() {
        return feelsLike;
    }

    /**
     * Returns relative humidity percentage.
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns wind speed in mph.
     */
    public double getWindSpeed() {
        return windSpeed;
    }

    /**
     * Returns solar irradiance in W/m^2.
     */
    public double getSolarIrradiance() {
        return solarIrradiance;
    }

    /**
     * Returns rainfall in inches.
     */
    public double getRainfall() {
        return rainfall;
    }

    /**
     * Returns timestamp for this weather reading.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether this reading came from cached/local data.
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Returns system/data status.
     */
    public SystemStatus getStatus() {
        return status;
    }

    /**
     * Returns timestamp formatted for display.
     *
     * If timestamp is missing, returns "--".
     */
    public String getFormattedTimestamp() {
        return timestamp == null ? "--" : timestamp.format(DISPLAY_FORMAT);
    }

    /**
     * Useful for debugging/logging WeatherData objects.
     */
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