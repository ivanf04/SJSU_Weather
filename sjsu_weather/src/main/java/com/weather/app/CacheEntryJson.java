package com.weather.app;

import java.time.LocalDate;
import java.util.List;


public class CacheEntryJson {
    String generatedOn;
    List<ForecastEntry> forecasts;

    static CacheEntryJson fromCacheEntry(CacheEntry e) {
        CacheEntryJson dto = new CacheEntryJson();
        dto.generatedOn = e.getGeneratedOn().toString();
        dto.forecasts   = e.getForecasts();
        return dto;
    }

    CacheEntry toCacheEntry() {
        return new CacheEntry(LocalDate.parse(generatedOn), forecasts);
    }
}
