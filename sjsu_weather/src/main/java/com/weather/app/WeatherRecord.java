package com.weather.app;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single raw weather record.
 *
 * This class stores data exactly as it appears in the CSV file:
 * - key = column name
 * - value = string value
 *
 * It is intentionally flexible (map-based) because:
 * - different weather sources may have different columns
 * - it avoids tightly coupling parsing logic to a fixed schema
 *
 * Later, WeatherRecord is converted into WeatherData (typed model).
 */
public class WeatherRecord {

    /**
     * Stores column-value pairs in insertion order.
     * LinkedHashMap preserves CSV column ordering.
     */
    private final Map<String, String> data = new LinkedHashMap<>();

    /**
     * Sets a value for a given column key.
     */
    public void setValue(String key, String value) {
        data.put(key, value);
    }

    /**
     * Retrieves a value by column key.
     *
     * Returns empty string if key is missing instead of null,
     * preventing NullPointerExceptions.
     */
    public String getValue(String key) {
        return data.getOrDefault(key, "");
    }
}