package com.weather.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV implementation of WeatherRepository.
 *
 * Responsibilities:
 * - store weather data in a CSV file
 * - append new rows
 * - retrieve first/last timestamps
 *
 * This class handles all file I/O operations.
 */
public class LocalCsvRepository implements WeatherRepository {

    /** File path for CSV storage */
    private final String fileName;

    /** Index of timestamp column in CSV */
    private final int timestampIndex = 2;

    public LocalCsvRepository(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean dataFileExists() {
        return new File(fileName).exists();
    }

    @Override
    public String getFirstTimestamp() {
        return getTimestampAtLine(1);
    }

    @Override
    public String getLastTimestamp() {
        File file = new File(fileName);
        if (!file.exists()) return "";

        String lastLine = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String current;
            while ((current = reader.readLine()) != null) {
                if (!current.trim().isEmpty()) lastLine = current;
            }
        } catch (IOException e) {
            return "";
        }

        return parseTimestampFromLine(lastLine);
    }

    @Override
    public void append(List<WeatherRecord> records, String[] headers) {

        boolean exists = new File(fileName).exists();

        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(fileName, true)))) {

            // Write header only if file is new
            if (!exists) writer.println(String.join(",", headers));

            // Write each record as CSV row
            for (WeatherRecord r : records) {
                List<String> row = new ArrayList<>();
                for (String h : headers) {
                    row.add("\"" + r.getValue(h) + "\"");
                }
                writer.println(String.join(",", row));
            }

        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    private String getTimestampAtLine(int lineIndex) {
        if (!new File(fileName).exists()) return "";

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            for (int i = 0; i < lineIndex; i++) reader.readLine();
            String line = reader.readLine();
            return line != null ? parseTimestampFromLine(line) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private String parseTimestampFromLine(String line) {
        String[] parts = line.replace("\"", "").split(",");
        return (parts.length > timestampIndex) ? parts[timestampIndex] : "";
    }
}