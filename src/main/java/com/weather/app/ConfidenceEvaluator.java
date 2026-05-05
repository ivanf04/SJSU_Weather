package com.weather.app;

import java.util.List;

/**
 * Evaluates forecast confidence based on: - historical temperature consistency
 * - trend strength - how many days ahead the prediction is
 */
public class ConfidenceEvaluator {

    private static final double HIGH_CONFIDENCE_STDDEV = 5.0;
    private static final double MEDIUM_CONFIDENCE_STDDEV = 8.0;
    private static final double STRONG_TREND_THRESHOLD = 2.0;

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

    public String evaluate(double stdDev, double trend, int daysAhead) {
        String confidence = baseConfidence(stdDev);

        if (Math.abs(trend) > STRONG_TREND_THRESHOLD) {
            confidence = degrade(confidence, 1);
        }

        if (daysAhead >= 5) {
            confidence = degrade(confidence, 1);
        } else if (daysAhead >= 3) {
            confidence = degrade(confidence, 0);
        }

        return confidence;
    }

    private String baseConfidence(double stdDev) {
        if (stdDev < HIGH_CONFIDENCE_STDDEV) {
            return "High";
        }
        if (stdDev < MEDIUM_CONFIDENCE_STDDEV) {
            return "Medium";
        }
        return "Low";
    }

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
