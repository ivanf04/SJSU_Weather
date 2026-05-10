package com.weather.app;

import java.nio.file.Files;

import javafx.application.Application;

/**
 * Main entry point for the application.
 *
 * Flow:
 * 1. Build WeatherAppComposition (single CSV path + shared forecast stack)
 * 2. Sync latest weather data from SJSU into that CSV
 * 3. Launch JavaFX dashboard with the provider wired from the same composition
 */
public class Main {
    public static void main(String[] args) {
        WeatherAppComposition composition = WeatherAppComposition.createDefault();

        composition.ensureCsvParentDirectoriesExist();

        System.out.println("Using CSV file: " + composition.getCsvPath());

        composition.runSync();

        if (!Files.exists(composition.getCsvPath())) {
            System.err.println("CSV file is still missing after sync. The dashboard will open but show no data until "
                    + "the file exists at the path above (check network and SJSU data page).");
        }

        WeatherDashboard.setInjectedDataProvider(composition.createDashboardDataProvider());
        Application.launch(WeatherDashboard.class);
    }
}
