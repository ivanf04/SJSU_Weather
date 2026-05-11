package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles forecast caching.
 *
 * This class acts as a facade for the forecast subsystem.
 *
 * It hides the details of:
 * - loading forecast cache
 * - checking whether cached forecast is fresh
 * - running the forecast model
 * - saving new forecast results
 *
 * If today's forecast is already cached, it returns the cached result.
 * Otherwise, it asks the configured ForecastModel to generate a fresh forecast.
 */
public class ArchiveClass {

    /**
     * Forecast algorithm strategy.
     *
     * This is an interface so different forecast models can be swapped in.
     */
    private final ForecastModel model;

    /**
     * Handles reading/writing forecast cache to disk.
     */
    private final ForecastCache cache;

    /**
     * Creates the forecast facade.
     *
     * @param model forecast algorithm
     * @param cache forecast cache storage
     */
    public ArchiveClass(ForecastModel model, ForecastCache cache) {
        this.model = model;
        this.cache = cache;
    }

    /**
     * Returns forecast results.
     *
     * Flow:
     * 1. Try to load cached forecast.
     * 2. If cache exists and is fresh, return it.
     * 3. Otherwise run the forecast model.
     * 4. Save new forecast to cache.
     * 5. Return fresh forecast.
     *
     * @param historicalData past weather data used to generate forecast
     * @return forecast entries
     */
    public List<ForecastEntry> getForecast(List<WeatherData> historicalData) {

        // Try loading existing cache.
        CacheEntry cached = cache.load();

        // Reuse cache if it was generated today.
        if (cached != null && cached.isFresh()) {
            System.out.println("Cache hit — returning forecast generated on " + cached.getGeneratedOn());
            return cached.getForecasts();
        }

        // If cache is missing or stale, run forecast model.
        System.out.println("Cache miss — running forecast model for " + LocalDate.now());

        List<ForecastEntry> freshForecasts = model.generateForecast(historicalData);

        // Only save non-empty forecasts.
        if (!freshForecasts.isEmpty()) {
            cache.save(new CacheEntry(LocalDate.now(), freshForecasts));
        }

        return freshForecasts;
    }

    /**
     * Deletes the forecast cache.
     *
     * Useful when user wants to force regeneration.
     */
    public void invalidateCache() {
        cache.invalidate();
    }

    /**
     * Returns the date the cache was generated, if cache exists.
     */
    public LocalDate getCacheDate() {
        CacheEntry cached = cache.load();
        return cached != null ? cached.getGeneratedOn() : null;
    }
}