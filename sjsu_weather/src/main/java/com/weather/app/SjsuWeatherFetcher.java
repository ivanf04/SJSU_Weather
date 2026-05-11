package com.weather.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Concrete implementation of WeatherSource.
 *
 * This class fetches weather data from the SJSU Meteorology website.
 *
 * Responsibilities:
 * - connect to SJSU weather index page
 * - locate the correct yearly CSV file
 * - download and parse CSV rows
 * - convert rows into WeatherRecord objects
 * - filter out already-seen data based on timestamp
 */
public class SjsuWeatherFetcher implements WeatherSource {

    /** Base URL for SJSU weather station data */
    private final String baseUrl;

    /** Stores CSV headers once read */
    private String[] headers;

    public SjsuWeatherFetcher(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<WeatherRecord> fetchAll(String lastTimestamp) {
        List<WeatherRecord> results = new ArrayList<>();

        try {
            // Load HTML page listing CSV files
            Document doc = Jsoup.connect(baseUrl).get();

            // Find correct CSV file for current or recent years
            String href = resolveYearlyCsvHref(doc);

            if (href == null || href.isBlank()) {
                System.err.println("SjsuWeatherFetcher: no .csv link found");
                return results;
            }

            // Build full URL
            String csvUrl = href.startsWith("http") ? href : baseUrl + href;

            // Read CSV file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new URL(csvUrl).openStream()))) {

                String line;
                boolean isHeader = true;

                while ((line = reader.readLine()) != null) {

                    String[] cols = line.replace("\"", "").split(",");

                    // First row is header
                    if (isHeader) {
                        headers = cols;
                        isHeader = false;
                        continue;
                    }

                    // Build record from row
                    WeatherRecord record = new WeatherRecord();

                    for (int i = 0; i < headers.length; i++) {
                        record.setValue(headers[i],
                                i < cols.length ? cols[i] : "");
                    }

                    // Only add new records (avoid duplicates)
                    if (lastTimestamp.isEmpty() ||
                            record.getValue("TIMESTAMP").compareTo(lastTimestamp) > 0) {
                        results.add(record);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("SjsuWeatherFetcher: download failed — " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("SjsuWeatherFetcher: unexpected error — " + e.getMessage());
        }

        return results;
    }

    /**
     * Finds the correct yearly CSV file.
     *
     * Strategy:
     * - try current year
     * - fallback to recent years
     * - fallback to any CSV
     */
    static String resolveYearlyCsvHref(Document doc) {
        int currentYear = Year.now().getValue();

        for (int y = currentYear; y >= currentYear - 3; y--) {
            Element a = doc.selectFirst("a[href$=\"" + y + ".csv\"]");
            if (a != null) return a.attr("href");
        }

        Element fallback = doc.selectFirst("a[href$=.csv]");
        return fallback != null ? fallback.attr("href") : null;
    }

    @Override
    public String[] getHeaders() {
        return headers;
    }
}