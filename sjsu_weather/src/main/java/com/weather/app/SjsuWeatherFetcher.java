package com.weather.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
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

            Element link = doc.select("a[href$=.csv]").first();
            if (link == null) {
                System.err.println("No CSV link found at: " + baseUrl);
                return results;
            }

            String csvUrl = baseUrl + link.attr("href");
            System.out.println("Fetching CSV from: " + csvUrl);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new URL(csvUrl).openStream()))) {

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

                    String timestamp = record.getValue("TIMESTAMP");
                    if (lastTimestamp == null || lastTimestamp.isEmpty() || timestamp.compareTo(lastTimestamp) > 0) {
                        results.add(record);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Weather fetch failed:");
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public String[] getHeaders() {
        return headers;
    }
}