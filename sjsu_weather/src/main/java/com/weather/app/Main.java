package com.weather.app;

public class Main {
    public static void main(String[] args) {
        WeatherDataService app = new WeatherDataService(
            new SjsuWeatherFetcher("https://www.met.sjsu.edu/weather/sfcdata/data/DHRoof/"),
            new LocalCsvRepository("sjsu_weather_backup.csv")
        );

        app.runSync();
    }
}