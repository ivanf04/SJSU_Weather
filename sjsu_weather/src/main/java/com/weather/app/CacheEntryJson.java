package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object (DTO) used for JSON serialization.
 *
 * ForecastCache writes/reads this object instead of directly writing CacheEntry.
 *
 * Why this exists:
 * - CacheEntry uses LocalDate
 * - JSON stores dates as strings
 * - This class converts between the two forms
 */
public class CacheEntryJson {

    /**
     * Date forecast was generated, stored as text for JSON.
     */
    String generatedOn;

    /**
     * Forecast list stored in JSON.
     */
    List<ForecastEntry> forecasts;

    /**
     * Converts an application CacheEntry into JSON-friendly DTO.
     */
    static CacheEntryJson fromCacheEntry(CacheEntry e) {
        CacheEntryJson dto = new CacheEntryJson();
        dto.generatedOn = e.getGeneratedOn().toString();
        dto.forecasts   = e.getForecasts();
        return dto;
    }

    /**
     * Converts JSON-friendly DTO back into application CacheEntry.
     */
    CacheEntry toCacheEntry() {
        return new CacheEntry(LocalDate.parse(generatedOn), forecasts);
    }
}