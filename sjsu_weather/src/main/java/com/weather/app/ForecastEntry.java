package com.weather.app;

import java.time.LocalDate;

/**
 * TEMPORARY STUB for ForecastEntry
 * TO DO: Real version will use PredictionEngine later
 *
 * This class represents ONE row in the forecast table in the UI.
 *
 * Example row in the UI:
 * Date        | Predicted Temp | Confidence
 * 2026-04-19  | 72.5°F         | High
 *
 */
public class ForecastEntry {

    private final LocalDate date;
    private final double predictedTemperature;
    private final String confidenceLabel;

    /**
     * @param date the forecast date
     * @param predictedTemperature predicted temperature for that day
     * @param confidenceLabel confidence level of prediction
     */
    public ForecastEntry(LocalDate date,
                         double predictedTemperature,
                         String confidenceLabel) {

        this.date = date;
        this.predictedTemperature = predictedTemperature;
        this.confidenceLabel = confidenceLabel;
    }

    /**
     * Returns the forecast date.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns predicted temperature value.
     */
    public double getPredictedTemperature() {
        return predictedTemperature;
    }

    /**
     * Returns confidence label for display in UI.
     */
    public String getConfidenceLabel() {
        return confidenceLabel;
    }
}