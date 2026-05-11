package com.weather.app;

import java.util.List;

/**
 * Strategy interface for forecast algorithms.
 *
 * This is the abstraction that allows the app to swap prediction models.
 *
 * For example, the app can use:
 * - PredictionEngine now
 * - a moving-average model later
 * - a machine learning model later
 *
 * ArchiveClass depends on this interface instead of a concrete prediction class,
 * which keeps caching logic separate from forecasting logic.
 */
public interface ForecastModel {

    /**
     * Generates forecast entries from historical weather data.
     *
     * @param historicalData past weather records used as input
     * @return list of forecast entries to display/cache
     */
    List<ForecastEntry> generateForecast(List<WeatherData> historicalData);
}