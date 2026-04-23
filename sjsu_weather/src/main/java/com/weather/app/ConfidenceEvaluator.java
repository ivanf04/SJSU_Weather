package com.weather.app;

import java.util.List;

/*
    ConfidenceEvaluator

    Responsibility: Determine how confident a forecast prediction is,
    based on historical data consistency and how far ahead the prediction is.

    Confidence logic:
    - Historical variance (stdDev) sets the base confidence level
    - Days further into the future degrade that confidence

    Used by PredictionEngine when building each ForecastEntry.
    TLDR: Answers the question "how much should we trust this prediction?"
*/
public class ConfidenceEvaluator {

    /*
    threhold:
    stdDev < 2.0 -> "High" (temps were very consistent, easy to predict)
    stdDev < 4.0 -> "Medium" (some ups and downs, moderate certainty)
    stdDev >= 4.0 -> "Low" (temps were all over the place, hard to predict)
    */
    private static final double LOW_STDDEV_THRESHOLD  = 2.0;
    private static final double HIGH_STDDEV_THRESHOLD = 4.0;

    /*
        Computes the population standard deviation of the daily averages.

        IDEA:
        Standard deviation measures how spread out the temperatures were historically.
        A low stdDev means temperatures were stable and consistent -> forecast is more trustworthy.
        A high stdDev means temperatures were unpredictable -> forecast should be trusted less.

        FORMULA (Population Standard Deviation):
        Step 1 - Find the mean (average) of all daily temperatures:
        mean = (t1 + t2 + ... + tN) / N

        Step 2 - Find the variance (average of squared differences from mean):
        variance = ((t1 - mean)^2 + (t2 - mean)^2 + ... + (tN - mean)^2) / N
        We square the differences so that negative and positive gaps don't cancel each other out.

        Step 3 - Standard deviation is the square root of variance:
        stdDev = sqrt(variance)
        We square root at the end to bring the unit back to °C (squaring changed it to °C^2).

        EXAMPLE with 3 days [20°C, 22°C, 25°C]:
        Step 1: mean = (20 + 22 + 25) / 3 = 22.33°C
        Step 2: variance = ((20 - 22.33)^2 + (22 - 22.33)^2 + (25 - 22.33)^2) / 3
        = (5.43 + 0.11 + 7.13) / 3
        = 12.67 / 3
        = 4.22

        Step 3: stdDev = sqrt(4.22) = 2.05°C

        A stdDev of 2.05°C is just above our LOW threshold (2.0) -> base confidence = "Medium"

        @param dailyAverages chronological list of daily average temps
        @return standard deviation in °C
    */
    public double computeStandardDeviation(List<Double> dailyAverages) {
        //need at least 2 days to measure any spread
        if (dailyAverages == null || dailyAverages.size() < 2) return 0;

        int n = dailyAverages.size();

        //compute the mean (plain average of all daily temperatures)
        double mean = dailyAverages.stream()
        .mapToDouble(v -> v) //convert each Double to a primitive double
        .average() //compute the average
        .orElse(0); //fallback to 0 if stream is somehow empty

        //compute variance
        //for each day, find how far it is from the mean, square that gap, then average all the squared gaps together
        double variance = dailyAverages.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2)) //(temperature - mean)^2
        .sum() / n; //divide by number of days

        //square root of variance gives us standard deviation in °C
        return Math.sqrt(variance);
    }

    /*
        Returns a confidence label ("High", "Medium", or "Low") for one forecast day.

        IDEA:
        Two factors reduce our confidence in a prediction:
        - Inconsistent historical data (high stdDev) -> harder to spot a reliable pattern
        - Predicting further into the future -> more room for error to build up

        We handle these separately:
        - stdDev sets the BASE confidence
        - daysAhead then degrades it if needed

        DEGRADATION RULES:
        Day 1-2 -> no change (short-term, most reliable)
        Day 3-4 -> drop 1 level (medium-term, less certain)
        Day 5 -> drop 2 levels (furthest out, least certain)

        EXAMPLE:
        stdDev = 1.5°C (below 2.0) -> base = "High", daysAhead = 4 -> drop 1 -> final = "Medium"
        stdDev = 3.0°C (below 4.0) -> base = "Medium", daysAhead = 5 -> drop 2 -> final = "Low"

        @param stdDev standard deviation of historical daily averages
        @param daysAhead how many days ahead this prediction is (1-5)
        @return "High", "Medium", or "Low"
    */
    public String evaluate(double stdDev, int daysAhead) {
        //set base confidence using stdDev thresholds. Lower stdDev = more consistent history = higher starting confidence
        String confidence;
        if (stdDev < LOW_STDDEV_THRESHOLD) {
            confidence = "High";    //temps were very stable historically
        } else if (stdDev < HIGH_STDDEV_THRESHOLD) {
            confidence = "Medium";  //temps had moderate variation
        } else {
            confidence = "Low";     //temps were highly unpredictable
        }

        //degrade confidence based on how far ahead we are predicting the further out, the less we can trust the forecast
        if (daysAhead >= 5) {
            confidence = degrade(confidence, 2);    //drop two levels for day 5
        } else if (daysAhead >= 3) {
            confidence = degrade(confidence, 1);    //drop one level for days 3-4
        }
        //  days 1-2 keep their base confidence unchanged
        return confidence;
    }

    /*
        Drops a confidence label down by a given number of levels.
        The scale only goes one direction and stops at "Low" (it's the floor).

        High -> Medium -> Low -> (stays Low, cannot go lower)

        @param confidence starting confidence label
        @param levels how many levels to drop
        @return degraded confidence label
    */
    private String degrade(String confidence, int levels) {
        // drop the confidence label one level at a time, up to 'levels' times
        for (int i = 0; i < levels; i++) {
            if ("High".equals(confidence)) {
                confidence = "Medium"; //High drops to Medium
            } else if ("Medium".equals(confidence)) {
                confidence = "Low"; //Medium drops to Low
            }
            // if already "Low", nothing changes
        }
        return confidence;
    }
}