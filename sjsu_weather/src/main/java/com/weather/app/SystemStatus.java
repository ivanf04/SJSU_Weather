package com.weather.app;

/**
 * Represents the state of weather data currently displayed in the UI.
 *
 * This enum is intentionally separated from WeatherDashboard so that:
 * - data/model classes (like WeatherData) do NOT depend on UI classes
 * - proper layer separation is maintained
 *
 * Each value describes how reliable or fresh the displayed data is.
 */
public enum SystemStatus {

    /** Data was freshly retrieved from the live source */
    LIVE,

    /** Data came from local cache instead of live source */
    CACHED,

    /** Data exists but is considered outdated */
    STALE,

    /** An error occurred retrieving or processing data */
    ERROR,

    /** Data is currently being loaded */
    LOADING
}