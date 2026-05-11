package com.weather.app;

/**
 * Represents summary values for one day of weather data.
 *
 * Right now the dashboard only uses high and low temperature,
 * but this class can be extended later with average temperature,
 * total rainfall, max wind, etc.
 */
public class DailySummary {

    /**
     * Highest temperature recorded for the day.
     */
    private final double highTemp;

    /**
     * Lowest temperature recorded for the day.
     */
    private final double lowTemp;

    /**
     * Creates a daily summary object.
     */
    public DailySummary(double highTemp, double lowTemp) {
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
    }

    /**
     * Returns high temperature for the day.
     */
    public double getHighTemp() {
        return highTemp;
    }

    /**
     * Returns low temperature for the day.
     */
    public double getLowTemp() {
        return lowTemp;
    }

    /**
     * Useful for debugging/logging summary values.
     */
    @Override
    public String toString() {
        return "DailySummary{" +
                "highTemp=" + highTemp +
                ", lowTemp=" + lowTemp +
                '}';
    }
}