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

public class SjsuWeatherFetcher implements WeatherSource {
    private final String baseUrl;
    private String[] headers;

    public SjsuWeatherFetcher(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<WeatherRecord> fetchAll(String lastTimestamp) {
        List<WeatherRecord> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(baseUrl).get();
            String href = resolveYearlyCsvHref(doc);
            if (href == null || href.isBlank()) {
                System.err.println("SjsuWeatherFetcher: no .csv link found on index page: " + baseUrl);
                return results;
            }

            String csvUrl = href.startsWith("http") ? href : baseUrl + href;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(csvUrl).openStream()))) {
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    String[] cols = line.replace("\"", "").split(",");
                    if (isHeader) {
                        headers = cols;
                        isHeader = false;
                        continue;
                    }

                    WeatherRecord record = new WeatherRecord();
                    for (int i = 0; i < headers.length; i++) {
                        record.setValue(headers[i], i < cols.length ? cols[i] : "");
                    }

                    if (lastTimestamp.isEmpty() || record.getValue("TIMESTAMP").compareTo(lastTimestamp) > 0) {
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
     * Picks a year-specific CSV (e.g. {@code 2026.csv}) from the roof data index, trying the
     * current year and recent past years, then any {@code .csv} link as a last resort.
     */
    static String resolveYearlyCsvHref(Document doc) {
        int currentYear = Year.now().getValue();
        for (int y = currentYear; y >= currentYear - 3; y--) {
            Element a = doc.selectFirst("a[href$=\"" + y + ".csv\"]");
            if (a != null) {
                return a.attr("href");
            }
        }
        Element fallback = doc.selectFirst("a[href$=.csv]");
        return fallback != null ? fallback.attr("href") : null;
    }

    @Override
    public String[] getHeaders() {
        return headers;
    }
}
