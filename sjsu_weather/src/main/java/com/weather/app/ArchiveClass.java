package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles forecast caching.
 *
 * If today's forecast is already cached, it returns the cached result.
 * Otherwise, it asks the configured ForecastModel to generate a fresh forecast.
 */
public class ArchiveClass {

    private final ForecastModel model;
    private final ForecastCache cache;

    public ArchiveClass(ForecastModel model, ForecastCache cache) {
        this.model = model;
        this.cache = cache;
    }

    public List<ForecastEntry> getForecast(List<WeatherData> historicalData) {
        CacheEntry cached = cache.load();

        if (cached != null && cached.isFresh()) {
            System.out.println("Cache hit — returning forecast generated on " + cached.getGeneratedOn());
            return cached.getForecasts();
        }

        System.out.println("Cache miss — running forecast model for " + LocalDate.now());

        List<ForecastEntry> freshForecasts = model.generateForecast(historicalData);

        if (!freshForecasts.isEmpty()) {
            cache.save(new CacheEntry(LocalDate.now(), freshForecasts));
        }

        return freshForecasts;
    }

    public void invalidateCache() {
        cache.invalidate();
    }

    public LocalDate getCacheDate() {
        CacheEntry cached = cache.load();
        return cached != null ? cached.getGeneratedOn() : null;
    }
}