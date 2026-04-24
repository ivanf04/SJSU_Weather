package com.weather.app;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Responsibility: Decide whether to return a cached forecast or delegate to
 * PredictionEngine for a fresh one.  
 *
 * Cache strategy: ONE set of predictions, rewritten each new calendar day.
 * If the cache was generated today, it is returned as-is.
 * If it is stale (different date) or missing, the engine runs and the result
 * overwrites the old cache file.
 */
public class ArchiveClass {

    private final PredictionEngine engine;
    private final ForecastCache    cache;

    
    public ArchiveClass() {
        this.engine = new PredictionEngine();       // 5-day horizon
        this.cache  = new ForecastCache();          // forecast_cache.json
    }

    /**
     * Will used for testing this class 
     */
    public ArchiveClass(PredictionEngine engine, ForecastCache cache) {
        this.engine = engine;
        this.cache  = cache;
    }

    
    /**
     * Returns a 5-day forecast, using the disk cache when available and fresh.
     *
     * Flow:
     *  1. Load cache from disk
     *  2. If cache exists AND was generated today > return cached forecasts
     *  3. Otherwise > run PredictionEngine, persist result, return fresh forecasts
     *
     */
    public List<ForecastEntry> getForecast(List<WeatherData> historicalData) {

        // --- Step 1: Try the cache first ---
        CacheEntry cached = cache.load();

        if (cached != null && cached.isFresh()) {
            System.out.println("Cache hit — returning forecast generated on " + cached.getGeneratedOn());
            return cached.getForecasts();
        }

        // --- Step 2: Cache is stale or missing — run the engine ---
       System.out.println("Cache miss — running PredictionEngine for " + LocalDate.now());

        List<ForecastEntry> freshForecasts = engine.generateForecast(historicalData);

        // --- Step 3: Persist and return ---
        if (!freshForecasts.isEmpty()) {
            CacheEntry newEntry = new CacheEntry(LocalDate.now(), freshForecasts);
            cache.save(newEntry);
        }

        return freshForecasts;
    }

    /**
     * Forces the next call to getForecast() to re-run the engine by deleting
     * the cache file.  Useful for a "Refresh" button in the GUI.
     */
    public void invalidateCache() {
        cache.invalidate();
        System.out.println("Cache manually invalidated — next getForecast() will recompute.");
    }

    /**
     * Returns the date the current cache was generated, or null if no valid
     * cache exists.  Useful for displaying "Last updated: …" in the GUI.
     */
    public LocalDate getCacheDate() {
        CacheEntry cached = cache.load();
        return (cached != null) ? cached.getGeneratedOn() : null;
    }
}