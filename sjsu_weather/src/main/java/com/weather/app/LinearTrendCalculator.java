package com.weather.app;

import java.util.List;

/**
 * Performs the temperature math used by PredictionEngine.
 *
 * Provides:
 * - weighted moving average for a baseline temperature
 * - linear regression slope for short-term trend
 *
 * This class is separated from PredictionEngine so the math can be tested
 * independently and replaced if needed.
 */
public class LinearTrendCalculator {

    /**
     * Number of recent days used in the weighted moving average.
     */
    private static final int MOVING_AVERAGE_WINDOW = 7;

    /**
     * Computes a weighted moving average over the most recent days.
     *
     * More recent days get higher weight than older days.
     *
     * Example idea:
     * - oldest day in window gets weight 1
     * - next day gets weight 2
     * - newest day gets highest weight
     *
     * This gives the forecast baseline more sensitivity to recent conditions.
     */
    public double computeWeightedMovingAverage(List<Double> dailyAverages) {

        // No data means no baseline can be computed
        if (dailyAverages == null || dailyAverages.isEmpty()) {
            return 0;
        }

        int size = dailyAverages.size();

        // Use up to the last 7 days, or fewer if less data exists
        int window = Math.min(MOVING_AVERAGE_WINDOW, size);

        // Start index of the selected recent window
        int start = size - window;

        double weightedSum = 0;
        double totalWeight = 0;

        // Move through the selected recent days
        for (int i = 0; i < window; i++) {

            // More recent values receive higher weights
            double weight = i + 1;

            weightedSum += dailyAverages.get(start + i) * weight;
            totalWeight += weight;
        }

        // Divide weighted total by total weights to get final baseline
        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }

    /**
     * Computes the slope of a least-squares linear regression line.
     *
     * Positive slope means warming trend.
     * Negative slope means cooling trend.
     *
     * The x-axis is the day index: 0, 1, 2, ...
     * The y-axis is daily average temperature.
     */
    public double computeLinearTrend(List<Double> dailyAverages) {

        // Need at least two points to calculate a slope
        if (dailyAverages == null || dailyAverages.size() < 2) {
            return 0;
        }

        int n = dailyAverages.size();

        // Sums needed for least-squares slope formula
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

        // Least-squares slope formula
        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }
}