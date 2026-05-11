package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw WeatherData records into daily average temperatures.
 *
 * The PredictionEngine works on one value per day, not every individual
 * timestamped reading. This class performs that conversion.
 *
 * Used by PredictionEngine before trend and forecast calculations.
 */
public class TemperatureAggregator {

    /**
     * Takes a list of WeatherData records and returns one average temperature
     * per calendar day in chronological order.
     *
     * Days with no data are skipped.
     *
     * @param data raw WeatherData records
     * @return list of daily average temperatures, oldest to newest
     */
    public List<Double> computeDailyAverages(List<WeatherData> data) {

        // Result list will contain one temperature average per day.
        List<Double> averages = new ArrayList<>();

        // If no data was passed in, return the empty list immediately.
        if (data == null || data.isEmpty()) return averages;

        // Find the earliest date in the dataset.
        LocalDate earliest = data.stream()
        .filter(w -> w.getTimestamp() != null)  // skip records with no timestamp
        .map(w -> w.getTimestamp().toLocalDate())   // extract just the date part
        .min(LocalDate::compareTo)  // find the oldest date
        .orElse(LocalDate.now().minusDays(6));  // fallback if list is empty

        // Find the latest date in the dataset.
        LocalDate latest = data.stream()
        .filter(w -> w.getTimestamp() != null)
        .map(w -> w.getTimestamp().toLocalDate())
        .max(LocalDate::compareTo)  // find the newest date
        .orElse(LocalDate.now());   // fallback to today

        // Go through every calendar day between earliest and latest.
        for (LocalDate day = earliest; !day.isAfter(latest); day = day.plusDays(1)) {
            final LocalDate current = day;

            // For this specific day, filter all records that belong to it,
            // grab their temperature values, and compute the average.
            double avg = data.stream()
                    .filter(w -> w.getTimestamp() != null)
                    .filter(w -> w.getTimestamp().toLocalDate().equals(current))
                    .mapToDouble(WeatherData::getTemperature)
                    .average()
                    .orElse(Double.NaN);

            // Skip days with no records.
            if (!Double.isNaN(avg)) {
                averages.add(avg);
            }
        }

        return averages;
    }
}