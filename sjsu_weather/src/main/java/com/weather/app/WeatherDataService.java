package com.weather.app;

import java.util.Collections;
import java.util.List;

/**
 * Coordinates syncing data from WeatherSource to WeatherRepository.
 *
 * This class:
 * - determines last known timestamp
 * - fetches only new records
 * - appends them to storage
 *
 * It does not:
 * - parse CSV (handled elsewhere)
 * - display data (handled by UI)
 */
public class WeatherDataService {

    private final WeatherSource source;
    private final WeatherRepository repository;

    public WeatherDataService(WeatherSource source, WeatherRepository repository) {
        this.source = source;
        this.repository = repository;
    }

    /**
     * Runs synchronization process.
     */
    public void runSync() {

        String startRange = repository.getFirstTimestamp();
        String lastSeen = repository.getLastTimestamp();

        // Fetch only new records
        List<WeatherRecord> newOnes = source.fetchAll(lastSeen);

        if (!newOnes.isEmpty()) {

            // Save new records
            repository.append(newOnes, source.getHeaders());

            String rangeStart = startRange.isEmpty()
                    ? newOnes.get(0).getValue("TIMESTAMP")
                    : startRange;

            String rangeEnd = newOnes.get(newOnes.size() - 1).getValue("TIMESTAMP");

            System.out.println("Records from " + rangeStart + " - " + rangeEnd + ", saved on disk");

        } else {

            String[] headers = source.getHeaders();

            // If file doesn't exist, create it with header
            if (!repository.dataFileExists() && headers != null && headers.length > 0) {
                repository.append(Collections.emptyList(), headers);
                System.out.println("Created CSV with header row");
            } else {
                System.out.println("No new data. Current range: "
                        + (startRange.isEmpty() ? "None" : startRange + " - " + lastSeen));
            }
        }
    }
}