package com.weather.app;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheEntryTest
 *
 * CacheEntry is a value object. We test:
 *   1. Getters return what was passed to the constructor
 *   2. isFresh() returns true when generatedOn == today
 *   3. isFresh() returns false when generatedOn is yesterday
 */
class CacheEntryTest {

    private List<ForecastEntry> sampleForecasts() {
        return List.of(
            new ForecastEntry(LocalDate.now().plusDays(1), 18.0, "HIGH"),
            new ForecastEntry(LocalDate.now().plusDays(2), 17.5, "MEDIUM")
        );
    }

    @Test
    void getters_returnConstructorValues() {
        LocalDate today = LocalDate.now();
        List<ForecastEntry> forecasts = sampleForecasts();

        CacheEntry entry = new CacheEntry(today, forecasts);

        assertEquals(today, entry.getGeneratedOn());
        assertEquals(forecasts, entry.getForecasts());
    }

    @Test
    void isFresh_returnsTrueWhenGeneratedToday() {
        CacheEntry entry = new CacheEntry(LocalDate.now(), sampleForecasts());
        assertTrue(entry.isFresh(), "Entry generated today should be fresh");
    }

    @Test
    void isFresh_returnsFalseWhenGeneratedYesterday() {
        CacheEntry entry = new CacheEntry(LocalDate.now().minusDays(1), sampleForecasts());
        assertFalse(entry.isFresh(), "Entry generated yesterday should not be fresh");
    }
}
