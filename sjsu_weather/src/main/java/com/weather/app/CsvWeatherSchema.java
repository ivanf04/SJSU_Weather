package com.weather.app;

/**
 * Defines which CSV columns map to app weather fields.
 *
 * This class makes CSV parsing more flexible because LocalCsvDataProvider
 * does not need to hardcode column names directly.
 *
 * If a different weather station or CSV format is added later, we can create
 * a different CsvWeatherSchema without rewriting the provider logic.
 */
public class CsvWeatherSchema {

    private final String timestampColumn;
    private final String temperatureColumn;
    private final String humidityColumn;
    private final String windSpeedColumn;
    private final String solarColumn;
    private final String rainfallColumn;

    /**
     * Creates a schema that maps CSV column names to the fields our app expects.
     *
     * @param timestampColumn CSV column containing timestamp
     * @param temperatureColumn CSV column containing temperature
     * @param humidityColumn CSV column containing humidity
     * @param windSpeedColumn CSV column containing wind speed
     * @param solarColumn CSV column containing solar irradiance
     * @param rainfallColumn CSV column containing rainfall
     */
    public CsvWeatherSchema(String timestampColumn,
                            String temperatureColumn,
                            String humidityColumn,
                            String windSpeedColumn,
                            String solarColumn,
                            String rainfallColumn) {
        this.timestampColumn = timestampColumn;
        this.temperatureColumn = temperatureColumn;
        this.humidityColumn = humidityColumn;
        this.windSpeedColumn = windSpeedColumn;
        this.solarColumn = solarColumn;
        this.rainfallColumn = rainfallColumn;
    }

    /**
     * Default schema for the SJSU Duncan Hall Roof weather station CSV.
     *
     * These names match the actual headers in sjsu_weather_backup.csv.
     */
    public static CsvWeatherSchema sjsuRoofSchema() {
        return new CsvWeatherSchema(
                "TIMESTAMP",
                "AirTF_Avg",
                "RH",
                "WindSpeed_mph_avg",
                "SlrW_Avg",
                "Rain_in_Tot"
        );
    }

    /**
     * Returns the CSV column name used for timestamps.
     */
    public String getTimestampColumn() {
        return timestampColumn;
    }

    /**
     * Returns the CSV column name used for temperature.
     */
    public String getTemperatureColumn() {
        return temperatureColumn;
    }

    /**
     * Returns the CSV column name used for humidity.
     */
    public String getHumidityColumn() {
        return humidityColumn;
    }

    /**
     * Returns the CSV column name used for wind speed.
     */
    public String getWindSpeedColumn() {
        return windSpeedColumn;
    }

    /**
     * Returns the CSV column name used for solar irradiance.
     */
    public String getSolarColumn() {
        return solarColumn;
    }

    /**
     * Returns the CSV column name used for rainfall.
     */
    public String getRainfallColumn() {
        return rainfallColumn;
    }
}