package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines exactly what the dashboard needs from the data/application layer.
 *
 * This interface is important because it keeps WeatherDashboard independent
 * from CSV parsing, repositories, forecast logic, and backend syncing.
 *
 * Any future data provider can implement this interface as long as it can
 * supply the same dashboard-ready data.
 */
public interface DashboardDataProvider {

    /**
     * Returns the most recent weather reading.
     *
     * Used by the current weather cards at the top of the dashboard.
     */
    WeatherData getCurrentWeather();

    /**
     * Returns weather records within a selected date range.
     *
     * Used by the historical data table.
     */
    List<WeatherData> getHistoricalWeather(LocalDate startDate, LocalDate endDate);

    /**
     * Returns weather records for the daily trend chart.
     *
     * In the current provider, this is based on the latest available date
     * in the dataset rather than the system clock.
     */
    List<WeatherData> getDailyTrend();

    /**
     * Returns weather records for the weekly trend chart.
     *
     * In the current provider, this represents a 7-day period ending on
     * the latest available date in the dataset.
     */
    List<WeatherData> getWeeklyTrend();

    /**
     * Returns summary statistics for a day.
     *
     * Currently used for high and low temperature.
     */
    DailySummary getDailySummary();

    /**
     * Returns forecast results for the forecast table.
     */
    List<ForecastEntry> getForecast();
}