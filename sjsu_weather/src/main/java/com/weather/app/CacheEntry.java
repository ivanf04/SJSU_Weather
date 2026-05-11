package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Holds one full set of forecast predictions alongside the
 * date they were generated.
 *
 * Wrapped by ForecastCache for persistence and read by ArchiveClass to
 * decide whether the cached data is still fresh.
 */
public class CacheEntry {

    /**
     * Date when this forecast was generated.
     */
    private final LocalDate generatedOn;

    /**
     * Forecast rows generated on that date.
     */
    private final List<ForecastEntry> forecasts;

    /**
     * Creates one cache entry.
     *
     * @param generatedOn date forecast was generated
     * @param forecasts forecast results
     */
    public CacheEntry(LocalDate generatedOn, List<ForecastEntry> forecasts) {
        this.generatedOn = generatedOn;
        this.forecasts   = forecasts;
    }

    /**
     * Returns generation date.
     */
    public LocalDate getGeneratedOn()
    { return generatedOn; }

    /**
     * Returns cached forecast list.
     */
    public List<ForecastEntry> getForecasts()
    { return forecasts; }

    /**
     * Returns true if this cache entry was generated today and can be reused
     * without re-running the prediction engine.
     */
    public boolean isFresh() {
        return LocalDate.now().isEqual(generatedOn);
    }

    /**
     * Useful for debugging/logging cache contents.
     */
    @Override
    public String toString() {
        return "CacheEntry{generatedOn=" + generatedOn
                + ", forecasts=" + forecasts + "}";
    }
}