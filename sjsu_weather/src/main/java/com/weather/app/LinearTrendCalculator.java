package com.weather.app;

import java.util.List;

/*
    LinearTrendCalculator

    Responsibility: Perform all mathematical calculations needed to project a temperature trend from daily averages.

    Provides:
    - Weighted moving average (baseline temperature)
    - Linear regression slope (trend direction and rate)

    Used by PredictionEngine to produce a predicted temperature for each forecast day.
    TLDR: A lot of math involve in calculating the linear line for trend
*/

public class LinearTrendCalculator {

    //  Number of recent days to include in the moving average.
    private static final int MOVING_AVERAGE_WINDOW = 7;

    /*
        Computes a weighted moving average over the last N days.

        IDEA:
        A normal average treats every day equally.
        A weighted average gives more importance to recent days, since yesterday's temperature matters more than last week's when predicting tomorrow.

        FORMULA:
        Assign a weight to each day based on how recent it is:
        oldest day in window -> weight 1
        next day → weight 2
        ...
        most recent day → weight N (highest)

        Formula: weighted average = (w1 * t1 + w2 * t2 + ... + wN * tN) / (w1 + w2 + ... + wN)

        EXAMPLE with 3 days [20°C, 22°C, 25°C]:
        weights = [1, 2, 3]
        top =  1 * 20 + 2 * 22 + 3 * 25) = 20 + 44 + 75 = 139
        bottom = (1 + 2 + 3) = 6
        result = 139 / 6 = 23.2°C <- pulled toward the recent 25°C

        @param dailyAverages chronological list of daily average temps
        @return weighted baseline temperature
    */

    public double computeWeightedMovingAverage(List<Double> dailyAverages) {
        //return 0 if there is nothing to calculate
        if (dailyAverages == null || dailyAverages.isEmpty()) return 0;

        int size = dailyAverages.size();

        int window = Math.min(MOVING_AVERAGE_WINDOW, size); //only look at the last 7 days (or fewer if we don't have 7 days of data)

        int start  = size - window; //start index so we only look at the most recent 'window' days

        double weightedSum = 0; //running total of (weight * temperature)
        double totalWeight = 0; //running total of weights (used as the divisor)

        for (int i = 0; i < window; i++) {
            double weight = i + 1;  //weight increases with each step: oldest = 1, newest = window (This means the most recent day has the highest influence)

            weightedSum += dailyAverages.get(start + i) * weight;   //multiply this day's temperature by its weight and add to the total
            totalWeight += weight;  //track the total weight so we can divide correctly at the end
        }

        // Divide weighted total by sum of weights to get the final average. Guard against divide by zero just in case
        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }

    /*
        Fits a least squares linear regression to the daily averages and returns the slope (temperature change per day).

        IDEA:
        Draw a straight line through all the daily temperature points.
        The slope of that line tells us whether temperatures are trending up (positive) or down (negative) and by how much per day.

        FORMULA (Least Squares Slope):
        Think of each day as a point on a graph:
        x = day number (0, 1, 2, 3 ...)
        y = average temperature for that day

        The slope formula is:
        m = (n * sumXY - sumX * sumY) / (n * sumX^2 - (sumX)^2)

        Where:
        n = number of days
        sumX = sum of all day numbers
        sumY = sum of all temperatures
        sumXY = sum of (day number * temperature) for each day
        sumX^2 = sum of (day number * day number) for each day

        EXAMPLE with 3 days [20°C, 22°C, 25°C]:
        Day 0 -> x = 0, y = 20 | Day 1 -> x = 1, y = 22 | Day 2 -> x = 2, y = 25

        sumX = 0 + 1 + 2 = 3
        sumY = 20 + 22 + 25 = 67
        sumXY = (0 * 20)+(1 * 22)+(2 * 25) = 0 + 22 + 50 = 72
        sumX^2 = (0^2)+(1^2)+(2^2) = 0 + 1 + 4 = 5
        n = 3

        m = (3 * 72 - 3 * 67) / (3 * 5 - 3^2)
        = (216 - 201) / (15 - 9)
        = 15 / 6
        = 2.5°C per day <- temperatures are rising around 2.5°C each day

        Why least squares?
        It finds the slope that minimizes total error across ALL days, so no single day has too much influence on the result.

        @param dailyAverages chronological list of daily average temps
        @return slope in °C per day (positive = warming, negative = cooling)
    */

    public double computeLinearTrend(List<Double> dailyAverages) {
        //need at least 2 points to draw a line. If not return 0
        if (dailyAverages == null || dailyAverages.size() < 2) return 0;

        int n = dailyAverages.size();

        double sumX  = 0;
        double sumY  = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX  += i;
            sumY  += dailyAverages.get(i);
            sumXY += i * dailyAverages.get(i);
            sumX2 += (double) i * i;
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) 
            return 0;

        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }
}