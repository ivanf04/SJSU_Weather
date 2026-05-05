package com.weather.app;

/**
 * Represents summary values for one day of weather data.
 *
 * Right now the dashboard only uses high and low temperature,
 * but this class can be extended later with average temperature,
 * total rainfall, max wind, etc.
 */
public class DailySummary {

    private final double highTemp;
    private final double lowTemp;

    /**
     * Creates a daily summary object.
     */
    public DailySummary(double highTemp, double lowTemp) {
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
    }

    public double getHighTemp() {
        return highTemp;
    }

    public double getLowTemp() {
        return lowTemp;
    }

    @Override
    public String toString() {
        return "DailySummary{" +
                "highTemp=" + highTemp +
                ", lowTemp=" + lowTemp +
                '}';
    }
}