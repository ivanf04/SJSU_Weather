package com.weather.app;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path csvPath = Paths.get(System.getProperty("user.dir"),"sjsu_weather_backup.csv")
                .toAbsolutePath();

        System.out.println("Using CSV file: " + csvPath);

        WeatherDataService app = new WeatherDataService(
            new SjsuWeatherFetcher("https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/"),
            new LocalCsvRepository(csvPath.toString())
        );

        app.runSync();

        Application.launch(WeatherDashboard.class, args);
    }
}