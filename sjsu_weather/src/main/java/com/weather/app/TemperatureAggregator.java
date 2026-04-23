package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
    TemperatureAggregator
    Responsibility: Group raw WeatherData records into chronological daily average temperatures.

    Used by PredictionEngine before any trend or forecast math.
 */
public class TemperatureAggregator {

    /*
        Takes a list of WeatherData records and returns one average temperature per calendar day in chronological order.
        Days with no data are skipped.

        @param data raw WeatherData records
        @return list of daily average temperatures, oldest to newest
    */

    public List<Double> computeDailyAverages(List<WeatherData> data) {
        List<Double> averages = new ArrayList<>();

        if (data == null || data.isEmpty()) return averages;    //if no data was passed in, return the empty list immediately

        LocalDate earliest = data.stream()
        .filter(w -> w.getTimestamp() != null)  //skip records with no timestamp
        .map(w -> w.getTimestamp().toLocalDate())   //extract just the date part
        .min(LocalDate::compareTo)  //find the oldest date
        .orElse(LocalDate.now().minusDays(6));  //fallback if list is empty

        LocalDate latest = data.stream()
        .filter(w -> w.getTimestamp() != null)
        .map(w -> w.getTimestamp().toLocalDate())
        .max(LocalDate::compareTo)  //find the newest date
        .orElse(LocalDate.now());   //fallback to today

        for (LocalDate day = earliest; !day.isAfter(latest); day = day.plusDays(1)) {   //go through every day calendar 
            final LocalDate current = day;

            //for this specific day, filter all records that belong to it, grab their temperature values, and compute the average
            double avg = data.stream()
                    .filter(w -> w.getTimestamp() != null)
                    .filter(w -> w.getTimestamp().toLocalDate().equals(current))
                    .mapToDouble(WeatherData::getTemperature)
                    .average()
                    .orElse(Double.NaN);

            if (!Double.isNaN(avg)) {   //skip if no records
                averages.add(avg);
            }
        }
        return averages;
    }
}