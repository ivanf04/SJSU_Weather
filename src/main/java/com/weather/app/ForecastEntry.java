package com.weather.app;

import java.time.LocalDate;

/**
 * ForecastEntry
 *
 * Represents one row in the forecast table.
 *
 * Used by:
 * - PredictionEngine (to generate forecasts)
 * - ArchiveClass (to cache forecasts)
 * - WeatherDashboard (to display forecasts)
 *
 * Example:
 * Date        | Predicted Temp | Confidence
 * 2026-04-19  | 72.5           | High
 */
public class ForecastEntry {

    /** The forecast date */
    private final LocalDate date;

    /** Predicted temperature for that date */
    private final double predictedTemperature;

    /** Confidence label ("High", "Medium", "Low") */
    private final String confidenceLabel;

    /**
     * Creates one forecast entry.
     */
    public ForecastEntry(LocalDate date,
                         double predictedTemperature,
                         String confidenceLabel) {

        this.date = date;
        this.predictedTemperature = predictedTemperature;
        this.confidenceLabel = confidenceLabel;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPredictedTemperature() {
        return predictedTemperature;
    }

    public String getConfidenceLabel() {
        return confidenceLabel;
    }

    /**
     * Returns a formatted string for UI display.
     * Example: "72.5"
     */
    public String getFormattedTemperature() {
        return String.format("%.2f", predictedTemperature);
    }

    @Override
    public String toString() {
        return "ForecastEntry{" +
                "date=" + date +
                ", predictedTemperature=" + predictedTemperature +
                ", confidenceLabel='" + confidenceLabel + '\'' +
                '}';
    }
}