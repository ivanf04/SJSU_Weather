package com.weather.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;

public class SjsuWeatherFetcher implements WeatherSource {
    private final String baseUrl;
    private String[] headers;

    public SjsuWeatherFetcher(String baseUrl) { this.baseUrl = baseUrl; }

    @Override
    public List<WeatherRecord> fetchAll(String lastTimestamp) {
        List<WeatherRecord> results = new ArrayList<>();
        try {
            String csvUrl = baseUrl + Jsoup.connect(baseUrl).get().select("a[href$=2026.csv]").first().attr("href");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(csvUrl).openStream()))) {
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    String[] cols = line.replace("\"", "").split(",");
                    if (isHeader) { headers = cols; isHeader = false; continue; }

                    WeatherRecord record = new WeatherRecord();
                    for (int i = 0; i < headers.length; i++) {
                        record.setValue(headers[i], i < cols.length ? cols[i] : "");
                    }

                    if (lastTimestamp.isEmpty() || record.getValue("TIMESTAMP").compareTo(lastTimestamp) > 0) {
                        results.add(record);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }

    @Override
    public String[] getHeaders() { return headers; }
}