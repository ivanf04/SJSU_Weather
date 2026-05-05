package com.weather.app;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines exactly what the dashboard needs from the data/application layer.
 */
public interface DashboardDataProvider {
    WeatherData getCurrentWeather();
    List<WeatherData> getHistoricalWeather(LocalDate startDate, LocalDate endDate);
    List<WeatherData> getDailyTrend();
    List<WeatherData> getWeeklyTrend();
    DailySummary getDailySummary();
    List<ForecastEntry> getForecast();
}