package com.weather.app;

import java.util.List;

/**
 * Interface representing storage for weather data.
 *
 * This allows different storage implementations:
 * - CSV file (current)
 * - database (future)
 * - cloud storage (future)
 */
public interface WeatherRepository {

    /** Returns the earliest timestamp in storage */
    String getFirstTimestamp();

    /** Returns the most recent timestamp in storage */
    String getLastTimestamp();

    /**
     * Appends new records to storage.
     *
     * @param records new weather records
     * @param headers column names
     */
    void append(List<WeatherRecord> records, String[] headers);

    /** Checks whether storage already exists */
    boolean dataFileExists();
}