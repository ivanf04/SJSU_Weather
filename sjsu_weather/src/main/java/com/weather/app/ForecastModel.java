package com.weather.app;

import java.util.List;

/**
 * Strategy interface for forecast algorithms.
 *
 * Any future prediction model can implement this interface and be plugged into
 * ArchiveClass without changing cache logic or UI code.
 */
public interface ForecastModel {
    List<ForecastEntry> generateForecast(List<WeatherData> historicalData);
}