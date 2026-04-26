package com.weather.app;

import java.util.List;

/**
 * Evaluates forecast confidence based on:
 * - historical temperature consistency (standard deviation)
 * - how many days ahead the prediction is
 */
public class ConfidenceEvaluator {

    private static final double HIGH_CONFIDENCE_STDDEV = 2.0;
    private static final double MEDIUM_CONFIDENCE_STDDEV = 4.0;

    /**
     * Computes population standard deviation from daily average temperatures.
     *
     * Lower standard deviation means more stable historical temperatures,
     * which usually means higher confidence in the forecast.
     */
    public double computeStandardDeviation(List<Double> dailyAverages) {
        if (dailyAverages == null || dailyAverages.size() < 2) {
            return 0;
        }

        double mean = dailyAverages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double variance = dailyAverages.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / dailyAverages.size();

        return Math.sqrt(variance);
    }

    /**
     * Returns a confidence label for a forecast day.
     *
     * Base confidence is determined by standard deviation:
     * - stdDev < 2.0  -> High
     * - stdDev < 4.0  -> Medium
     * - stdDev >= 4.0 -> Low
     *
     * Then confidence is reduced for predictions farther into the future:
     * - days 1-2: no reduction
     * - days 3-4: reduce by 1 level
     * - day 5+: reduce by 2 levels
     */
    public String evaluate(double stdDev, int daysAhead) {
        String confidence = baseConfidence(stdDev);

        if (daysAhead >= 5) {
            return degrade(confidence, 2);
        } else if (daysAhead >= 3) {
            return degrade(confidence, 1);
        }

        return confidence;
    }

    /**
     * Determines the starting confidence level from standard deviation.
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
     * Lowers confidence by the specified number of levels.
     *
     * High -> Medium -> Low
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