package com.weather.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 *
 * Read and write a single CacheEntry to/from a JSON file.
 * This class knows about the file system and JSON serialisation.
 *
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

    public ForecastCache() {
        this(DEFAULT_CACHE_PATH);
    }

    /**
     * Will help with unit testing the class 
     */
    public ForecastCache(String filePath) {
        this.cachePath = Paths.get(filePath);
        this.gson      = buildGson();
    }

    public CacheEntry load() {
        if (!Files.exists(cachePath)) {
            System.out.println("No cache file found");
            return null;
        }

        try (FileReader reader = new FileReader(cachePath.toFile())) {
            Type type = new TypeToken<CacheEntryJson>() {}.getType();
            CacheEntryJson raw = gson.fromJson(reader, type);
            return raw == null ? null : raw.toCacheEntry();
        } catch (IOException e) {
            System.out.println("Failed to read cache file — will regenerate forecast.");
            return null;
        }
    }


    public void save(CacheEntry entry) {
        try (FileWriter writer = new FileWriter(cachePath.toFile())) {
            gson.toJson(CacheEntryJson.fromCacheEntry(entry), writer);
           System.err.println("Forecast cache saved to: " + cachePath);
        } catch (IOException e) {
            System.out.println ("Failed to write cache file — forecast will still display.");
        }
    }

    /**
     * Deletes the cache file (useful for forcing a refresh in tests).
     */
    public void invalidate() {
        try {
            Files.deleteIfExists(cachePath);
    
        } catch (IOException e) {
            System.out.println( "Failed to delete cache file.");
        }
    }

    
    private Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, t, ctx) ->
                                ctx.serialize(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) ->
                                LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

}

