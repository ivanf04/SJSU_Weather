package com.weather.app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



class ArchiveClassTest {

    private static final String TEST_CACHE_PATH = "test_archive_cache.json";

    private ForecastCache  cache;
    private PredictionEngine engine;
    private ArchiveClass   archive;
    private List<WeatherData> fakeHistory;

    @BeforeEach
    void setUp() {
        cache      = new ForecastCache(TEST_CACHE_PATH);
        engine     = new PredictionEngine();
        archive    = new ArchiveClass(engine, cache);
        fakeHistory = buildFakeHistory();
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Paths.get(TEST_CACHE_PATH));
    }

    // -------------------------------------------------------------------------
    // 1. Cache miss — engine runs and result is persisted
    // -------------------------------------------------------------------------

    @Test
    void getForecast_engineRuns_whenNoCacheExists() {
        // No cache file exists yet
        List<ForecastEntry> result = archive.getForecast(fakeHistory);

        assertNotNull(result, "Result should never be null");
        assertFalse(result.isEmpty(), "Engine should produce forecasts from valid historical data");
    }

    @Test
    void getForecast_savesCacheAfterEnginRun() {
        archive.getForecast(fakeHistory);

        // Cache file should now exist on disk
        assertTrue(Files.exists(Paths.get(TEST_CACHE_PATH)),
                "Cache file should be written after engine runs");
    }

    @Test
    void getForecast_savedCacheIsMarkedToday() {
        archive.getForecast(fakeHistory);

        CacheEntry saved = cache.load();
        assertNotNull(saved);
        assertEquals(LocalDate.now(), saved.getGeneratedOn(),
                "Saved cache should be stamped with today's date");
    }

    // -------------------------------------------------------------------------
    // 2. Cache hit — cached values returned, no re-computation
    // -------------------------------------------------------------------------

    @Test
    void getForecast_returnsCachedValues_whenCacheIsFresh() {
        // Pre-seed the cache with known values
        List<ForecastEntry> known = List.of(
            new ForecastEntry(LocalDate.now().plusDays(1), 99.9, "HIGH"),
            new ForecastEntry(LocalDate.now().plusDays(2), 88.8, "MEDIUM")
        );
        cache.save(new CacheEntry(LocalDate.now(), known));

        List<ForecastEntry> result = archive.getForecast(fakeHistory);

        // Should get the seeded values back, not new engine output
        assertEquals(2, result.size(), "Should return exactly the cached entries");
        assertEquals(99.9, result.get(0).getPredictedTemperature(), 0.001,
                "Should return the cached temperature, not a freshly computed one");
    }

    // -------------------------------------------------------------------------
    // 3. Invalidate — clears cache, forces engine on next call
    // -------------------------------------------------------------------------

    @Test
    void invalidateCache_thenGetForecast_runsEngineAgain() {
        // First call seeds the cache
        archive.getForecast(fakeHistory);
        assertTrue(Files.exists(Paths.get(TEST_CACHE_PATH)));

        // Invalidate, then call again
        archive.invalidateCache();
        List<ForecastEntry> result = archive.getForecast(fakeHistory);

        assertFalse(result.isEmpty(), "Engine should run and return results after invalidation");
    }

    @Test
    void invalidateCache_removesFileFromDisk() {
        archive.getForecast(fakeHistory);
        archive.invalidateCache();

        assertFalse(Files.exists(Paths.get(TEST_CACHE_PATH)),
                "Cache file should be deleted after invalidation");
    }

    // -------------------------------------------------------------------------
    // 4. Stale cache — yesterday's cache is treated as a miss
    // -------------------------------------------------------------------------

    @Test
    void getForecast_engineRuns_whenCacheIsStale() {
        // Seed a stale cache with a recognisable sentinel value
        List<ForecastEntry> staleEntries = List.of(
            new ForecastEntry(LocalDate.now().plusDays(1), 55.5, "LOW")
        );
        cache.save(new CacheEntry(LocalDate.now().minusDays(1), staleEntries));

        List<ForecastEntry> result = archive.getForecast(fakeHistory);

        // The engine should have run — result should NOT be the stale sentinel
        assertNotEquals(55.5, result.get(0).getPredictedTemperature(),
                "Stale cache should be ignored — engine should produce fresh values");
    }



    // -------------------------------------------------------------------------
    // 6. getCacheDate() helper
    // -------------------------------------------------------------------------

    @Test
    void getCacheDate_returnsNull_whenNoCacheExists() {
        assertNull(archive.getCacheDate(), "Should return null when no cache file exists");
    }

    @Test
    void getCacheDate_returnsToday_afterForecastRun() {
        archive.getForecast(fakeHistory);
        assertEquals(LocalDate.now(), archive.getCacheDate(),
                "getCacheDate() should return today after a successful forecast run");
    }

    // -------------------------------------------------------------------------
    // Helper — minimal WeatherData list the engine can work with
    // -------------------------------------------------------------------------

    private List<WeatherData> buildFakeHistory() {
        List<WeatherData> data = new ArrayList<>();
        double[] temps = { 14.0, 15.5, 13.8, 16.2, 17.0, 16.5, 18.1, 17.4, 19.0, 18.6 };

        for (int i = 0; i < temps.length; i++) {
            LocalDateTime timestamp = LocalDate.now().minusDays(temps.length - i).atTime(12, 0);
            data.add(new WeatherData(
                temps[i], temps[i] - 1, 65, 6.0, 150.0, 0.0,
                timestamp, false, SystemStatus.LIVE
            ));
        }
        return data;
    }
}
