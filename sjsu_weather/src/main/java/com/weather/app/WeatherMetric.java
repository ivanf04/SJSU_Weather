package com.weather.app;

/**
 * Represents different weather metrics that can be visualized.
 *
 * This enum is used by WeatherTrendChart to determine:
 * - which value from WeatherData to plot
 *
 * This makes the chart reusable instead of being hardcoded
 * to temperature only.
 */
public enum WeatherMetric {

    /** Temperature values (°F) */
    TEMPERATURE,

    /** Humidity percentage (%) */
    HUMIDITY,

    /** Wind speed (mph) */
    WIND_SPEED,

    /** Solar irradiance (W/m^2) */
    SOLAR,

    /** Rainfall (inches) */
    RAINFALL
}