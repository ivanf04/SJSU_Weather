package com.weather.app;

import javafx.application.Application;

/**
 * Main entry point for the application.
 *
 * Flow:
 * 1. Sync latest weather data from SJSU into local CSV
 * 2. Launch JavaFX dashboard
 */
public class Main {
    public static void main(String[] args) {
        WeatherDataService app = new WeatherDataService(
            new SjsuWeatherFetcher("https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/"),
            new LocalCsvRepository("sjsu_weather_backup.csv")
        );

        // Pull latest backend data into local storage first
        app.runSync();

        // Then launch the frontend
        Application.launch(WeatherDashboard.class, args);
    }
}