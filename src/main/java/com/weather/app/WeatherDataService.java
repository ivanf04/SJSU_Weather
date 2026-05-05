package com.weather.app;

import java.util.List;

public class WeatherDataService {
    private final WeatherSource source;
    private final WeatherRepository repository;

    public WeatherDataService(WeatherSource source, WeatherRepository repository) {
        this.source = source;
        this.repository = repository;
    }

    public void runSync() {
        String startRange = repository.getFirstTimestamp();
        String lastSeen = repository.getLastTimestamp();

        List<WeatherRecord> newOnes = source.fetchAll(lastSeen);

        if (!newOnes.isEmpty()) {
            repository.append(newOnes, source.getHeaders());
            String rangeStart = startRange.isEmpty() ? newOnes.get(0).getValue("TIMESTAMP") : startRange;
            String rangeEnd = newOnes.get(newOnes.size() - 1).getValue("TIMESTAMP");
            System.out.println("Records from " + rangeStart + " - " + rangeEnd + ", saved on disk");
        } else {
            System.out.println("No new data. Current range: " + (startRange.isEmpty() ? "None" : startRange + " - " + lastSeen));
        }
    }
}