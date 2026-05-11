package com.weather.app;

/**
 * Represents the state of weather data.
 *
 * Kept outside WeatherDashboard so model/data classes do not depend on the UI.
 */
public enum SystemStatus {
    LIVE,
    CACHED,
    STALE,
    ERROR,
    LOADING
}