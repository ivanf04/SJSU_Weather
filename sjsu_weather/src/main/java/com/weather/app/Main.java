package com.weather.app;

import java.nio.file.Files;

import javafx.application.Application;

/**
 * Main entry point for the application.
 *
 * Overall flow of the program:
 *
 * 1. Create the application composition (all dependencies wired together)
 * 2. Ensure the CSV storage directory exists
 * 3. Sync latest weather data from SJSU into the local CSV file
 * 4. Launch the JavaFX dashboard using the same data provider
 *
 * This ensures that:
 * - backend and UI use the same data source
 * - forecast/cache logic is shared across the app
 */

public class Main {
    /**
     * Entry point of the application.
     */
    public static void main(String[] args) {

        // Build the full application configuration (composition root)
        WeatherAppComposition composition = WeatherAppComposition.createDefault();

        // Ensure the CSV directory exists before writing data
        composition.ensureCsvParentDirectoriesExist();

        // Print path being used for debugging / clarity
        System.out.println("Using CSV file: " + composition.getCsvPath());

        // Sync latest weather data from remote source into CSV
        composition.runSync();

        // If CSV still doesn't exist, warn user (but continue to launch UI)
        if (!Files.exists(composition.getCsvPath())) {
            System.err.println("CSV file is still missing after sync. The dashboard will open but show no data until "
                    + "the file exists at the path above (check network and SJSU data page).");
        }

        // Inject the same data provider into the dashboard
        // This ensures UI uses the exact same configuration as backend
        WeatherDashboard.setInjectedDataProvider(composition.createDashboardDataProvider());

        // Launch JavaFX application
        Application.launch(WeatherDashboard.class);
    }
}