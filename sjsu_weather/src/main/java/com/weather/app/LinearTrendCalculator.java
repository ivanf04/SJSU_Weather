package com.weather.app;

import java.util.List;

/**
 * Performs the temperature math used by PredictionEngine.
 *
 * Provides:
 * - weighted moving average for a baseline temperature
 * - linear regression slope for short-term trend
 */
public class LinearTrendCalculator {

    private static final int MOVING_AVERAGE_WINDOW = 7;

    /**
     * Computes a weighted moving average over the most recent days.
     *
     * More recent days get higher weight than older days.
     */
    public double computeWeightedMovingAverage(List<Double> dailyAverages) {
        if (dailyAverages == null || dailyAverages.isEmpty()) {
            return 0;
        }

        int size = dailyAverages.size();
        int window = Math.min(MOVING_AVERAGE_WINDOW, size);
        int start = size - window;

        double weightedSum = 0;
        double totalWeight = 0;

        for (int i = 0; i < window; i++) {
            double weight = i + 1;
            weightedSum += dailyAverages.get(start + i) * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }

    /**
     * Computes the slope of a least-squares linear regression line.
     *
     * Positive slope means warming trend.
     * Negative slope means cooling trend.
     */
    public double computeLinearTrend(List<Double> dailyAverages) {
        if (dailyAverages == null || dailyAverages.size() < 2) {
            return 0;
        }

        int n = dailyAverages.size();

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double y = dailyAverages.get(i);
            sumX += i;
            sumY += y;
            sumXY += i * y;
            sumX2 += (double) i * i;
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) {
            return 0;
        }

        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }
}