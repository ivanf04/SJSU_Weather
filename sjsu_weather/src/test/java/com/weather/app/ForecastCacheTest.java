package com.weather.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ForecastCacheTest
 *
 * ForecastCache does disk I/O. We use a dedicated temp file path so tests
 * never touch the real forecast_cache.json. The file is deleted after each test.
 *
 * We test:
 *   1. load() returns null when no file exists
 *   2. save() then load() round-trips the data correctly
 *   3. load() preserves the date and all forecast entries
 *   4. invalidate() deletes the file
 *   5. load() after invalidate() returns null
 *   6. Saving overwrites a previous entry (one set, not accumulating)
 */
class ForecastCacheTest {

    // Isolated test file — never touches real forecast_cache.json
    private static final String TEST_CACHE_PATH = "test_forecast_cache.json";

    private ForecastCache cache;

    @BeforeEach
    void setUp() {
        cache = new ForecastCache(TEST_CACHE_PATH);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Always clean up the test file so tests don't bleed into each other
        Files.deleteIfExists(Paths.get(TEST_CACHE_PATH));
    }


    @Test
    void load_returnsNull_whenNoFileExists() {
        // No file written — load should return null gracefully
        assertNull(cache.load(), "Should return null when cache file does not exist");
    }

    @Test
    void saveAndLoad_roundTripsDateCorrectly() {
        LocalDate today = LocalDate.now();
        CacheEntry original = new CacheEntry(today, sampleForecasts());

        cache.save(original);
        CacheEntry loaded = cache.load();

        assertNotNull(loaded, "Loaded entry should not be null after save");
        assertEquals(today, loaded.getGeneratedOn(), "generatedOn date should survive save/load");
    }

    @Test
    void saveAndLoad_roundTripsForecastEntries() {
        List<ForecastEntry> forecasts = sampleForecasts();
        CacheEntry original = new CacheEntry(LocalDate.now(), forecasts);

        cache.save(original);
        CacheEntry loaded = cache.load();

        assertNotNull(loaded);
        assertEquals(forecasts.size(), loaded.getForecasts().size(),
                "Number of forecast entries should survive save/load");

        ForecastEntry first = loaded.getForecasts().get(0);
        assertEquals(forecasts.get(0).getPredictedTemperature(),
                first.getPredictedTemperature(), 0.001,
                "Predicted temperature should survive save/load");
        assertEquals(forecasts.get(0).getConfidenceLabel(),
                first.getConfidenceLabel(),
                "Confidence label should survive save/load");
    }

    @Test
    void invalidate_deletesTheCacheFile() throws Exception {
        cache.save(new CacheEntry(LocalDate.now(), sampleForecasts()));
        assertTrue(Files.exists(Paths.get(TEST_CACHE_PATH)), "File should exist before invalidate");

        cache.invalidate();

        assertFalse(Files.exists(Paths.get(TEST_CACHE_PATH)), "File should be gone after invalidate");
    }

    @Test
    void load_returnsNull_afterInvalidate() {
        cache.save(new CacheEntry(LocalDate.now(), sampleForecasts()));
        cache.invalidate();

        assertNull(cache.load(), "load() should return null after invalidate");
    }

    @Test
    void save_overwritesPreviousEntry() {
        // Save a stale entry from yesterday
        CacheEntry stale = new CacheEntry(LocalDate.now().minusDays(1), sampleForecasts());
        cache.save(stale);

        // Overwrite with today's entry
        CacheEntry fresh = new CacheEntry(LocalDate.now(), sampleForecasts());
        cache.save(fresh);

        CacheEntry loaded = cache.load();
        assertNotNull(loaded);
        assertEquals(LocalDate.now(), loaded.getGeneratedOn(),
                "Second save should overwrite the first — only one entry persists");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private List<ForecastEntry> sampleForecasts() {
        return List.of(
            new ForecastEntry(LocalDate.now().plusDays(1), 18.0, "HIGH"),
            new ForecastEntry(LocalDate.now().plusDays(2), 17.5, "MEDIUM"),
            new ForecastEntry(LocalDate.now().plusDays(3), 16.8, "LOW")
        );
    }
}
