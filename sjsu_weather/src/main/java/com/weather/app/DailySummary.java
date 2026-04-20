package com.weather.app;

/**
 * TEMPORARY STUB for DailySummary
 *
 * Represents summary statistics for a single day.
 *
 * Example use in UI:
 * Daily High / Low: 75°F / 62°F
 *
 */
public class DailySummary {

    private final double highTemp;
    private final double lowTemp;

    /**
     * @param highTemp highest temperature of the day
     * @param lowTemp lowest temperature of the day
     */
    public DailySummary(double highTemp, double lowTemp) {
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
    }

    /**
     * Returns the daily high temperature.
     */
    public double getHighTemp() {
        return highTemp;
    }

    /**
     * Returns the daily low temperature.
     */
    public double getLowTemp() {
        return lowTemp;
    }
}