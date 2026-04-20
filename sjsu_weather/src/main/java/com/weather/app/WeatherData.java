package com.weather.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TEMPORARY STUB for WeatherData
 * 
 * Purpose:
 * - Provide typed fields for the UI
 * - Match what WeatherDashboard expects
 */
public class WeatherData {

    /* ---------- Core weather fields ---------- */

    private double temperature;
    private double feelsLike;
    private double humidity;
    private double windSpeed;
    private double solarIrradiance;
    private double rainfall;

    /* ---------- Metadata ---------- */

    private LocalDateTime timestamp;
    private boolean cached;
    private WeatherDashboard.SystemStatus status;

    /**
     * Constructor used by the UI (for now)
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

    /* ---------- Getters (used by UI) ---------- */

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
     * Formats timestamp for display in UI.
     */
    public String getFormattedTimestamp() {
        if (timestamp == null) return "--";
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}