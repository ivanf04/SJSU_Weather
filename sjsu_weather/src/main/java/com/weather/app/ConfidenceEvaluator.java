package com.weather.app;

import java.util.List;

/**
 * Evaluates forecast confidence.
 *
 * Confidence is based on:
 * - historical temperature consistency
 * - trend strength
 * - how many days ahead the prediction is
 *
 * Lower variation usually means higher confidence.
 * Strong trend or farther forecast date can reduce confidence.
 */
public class ConfidenceEvaluator {

    /**
     * Standard deviation below this value starts as High confidence.
     */
    private static final double HIGH_CONFIDENCE_STDDEV = 5.0;

    /**
     * Standard deviation below this value starts as Medium confidence.
     * Values above this become Low confidence.
     */
    private static final double MEDIUM_CONFIDENCE_STDDEV = 8.0;

    /**
     * If absolute trend is greater than this, confidence is degraded.
     *
     * Reason: a strong warming/cooling trend means conditions are changing quickly,
     * which makes the forecast less stable.
     */
    private static final double STRONG_TREND_THRESHOLD = 2.0;

    /**
     * Computes standard deviation of daily average temperatures.
     *
     * Standard deviation measures spread:
     * - low stdDev = temperatures were consistent
     * - high stdDev = temperatures varied a lot
     *
     * @param dailyAverages recent daily average temperatures
     * @return standard deviation
     */
    public double computeStandardDeviation(List<Double> dailyAverages) {

        // Need at least two values to measure spread.
        if (dailyAverages == null || dailyAverages.size() < 2) {
            return 0;
        }

        // Mean = average of all daily average temperatures.
        double mean = dailyAverages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        // Variance = average squared distance from the mean.
        double variance = dailyAverages.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / dailyAverages.size();

        // Standard deviation is square root of variance.
        return Math.sqrt(variance);
    }

    /**
     * Produces a confidence label for one forecast day.
     *
     * @param stdDev spread of recent daily averages
     * @param trend temperature change per day
     * @param daysAhead how far into the future the forecast is
     * @return "High", "Medium", or "Low"
     */
    public String evaluate(double stdDev, double trend, int daysAhead) {

        // First determine confidence from historical consistency.
        String confidence = baseConfidence(stdDev);

        // Strong trend reduces confidence because data is changing quickly.
        if (Math.abs(trend) > STRONG_TREND_THRESHOLD) {
            confidence = degrade(confidence, 1);
        }

        // Forecasts farther out are less certain.
        if (daysAhead >= 5) {
            confidence = degrade(confidence, 1);
        } else if (daysAhead >= 3) {
            confidence = degrade(confidence, 0);
        }

        return confidence;
    }

    /**
     * Converts standard deviation into starting confidence.
     */
    private String baseConfidence(double stdDev) {

        if (stdDev < HIGH_CONFIDENCE_STDDEV) {
            return "High";
        }

        if (stdDev < MEDIUM_CONFIDENCE_STDDEV) {
            return "Medium";
        }

        return "Low";
    }

    /**
     * Drops confidence by a number of levels.
     *
     * Scale:
     * High -> Medium -> Low
     *
     * Low is the floor and cannot go lower.
     */
    private String degrade(String confidence, int levels) {

        for (int i = 0; i < levels; i++) {
            if ("High".equals(confidence)) {
                confidence = "Medium";
            } else if ("Medium".equals(confidence)) {
                confidence = "Low";
            }
        }

        return confidence;
    }
}