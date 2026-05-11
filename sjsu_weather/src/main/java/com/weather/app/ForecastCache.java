package com.weather.app;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * Reads and writes a single CacheEntry to/from a JSON file.
 *
 * This class knows about:
 * - file system access
 * - JSON serialization
 * - JSON deserialization
 *
 * It does NOT generate forecasts itself.
 * It only persists forecast results.
 *
 * File format (forecast_cache.json):
 * {
 *   "generatedOn": "2025-06-01",
 *   "forecasts": [
 *     { "date": "2025-06-02", "predictedTemperature": 18.4, "confidence": "HIGH" },
 *     ...
 *   ]
 * }
 */
public class ForecastCache {

    private static final String DEFAULT_CACHE_PATH = "forecast_cache.json";

    private final Path cachePath;
    private final Gson gson;

    /**
     * Default constructor using forecast_cache.json.
     */
    public ForecastCache() {
        this(DEFAULT_CACHE_PATH);
    }

    /**
     * Constructor with custom path, useful for testing.
     */
    public ForecastCache(String filePath) {
        this.cachePath = Paths.get(filePath);
        this.gson = buildGson();
    }

    /**
     * Loads cached forecast data from disk.
     * Returns null if the file does not exist or cannot be read.
     */
    public CacheEntry load() {
        if (!Files.exists(cachePath)) {
            return null;
        }

        try (FileReader reader = new FileReader(cachePath.toFile())) {
            Type type = new TypeToken<CacheEntryJson>() {}.getType();
            CacheEntryJson raw = gson.fromJson(reader, type);
            return raw == null ? null : raw.toCacheEntry();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Saves forecast data to disk.
     */
    public void save(CacheEntry entry) {
        try (FileWriter writer = new FileWriter(cachePath.toFile())) {
            gson.toJson(CacheEntryJson.fromCacheEntry(entry), writer);
        } catch (IOException e) {
            System.out.println("Failed to write forecast cache.");
        }
    }

    /**
     * Deletes the cache file if it exists.
     */
    public void invalidate() {
        try {
            Files.deleteIfExists(cachePath);
        } catch (IOException e) {
            System.out.println("Failed to delete forecast cache.");
        }
    }

    /**
     * Builds Gson with LocalDate serialization support.
     */
    private Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) -> LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }
}