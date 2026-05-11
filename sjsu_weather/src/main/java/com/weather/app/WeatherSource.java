package com.weather.app;

import java.util.List;

/**
 * Interface representing a source of weather data.
 *
 * This abstraction allows the application to support multiple weather sources
 * without changing the rest of the system.
 *
 * Examples of implementations:
 * - SjsuWeatherFetcher (current)
 * - AirportWeatherFetcher (future)
 */
public interface WeatherSource {

    /**
     * Fetches all new weather records from the source.
     *
     * @param lastTimestamp the latest timestamp already stored locally
     *                      (used to avoid duplicate data)
     * @return list of new WeatherRecord objects
     */
    List<WeatherRecord> fetchAll(String lastTimestamp);

    /**
     * Returns the CSV headers associated with this data source.
     *
     * Needed so the repository can correctly write CSV columns.
     */
    String[] getHeaders();
}