package com.weather.app;

/**
 * Defines which CSV columns map to app weather fields.
 *
 * This allows the data provider to support different CSV formats without
 * rewriting parsing logic.
 */
public class CsvWeatherSchema {

    private final String timestampColumn;
    private final String temperatureColumn;
    private final String humidityColumn;
    private final String windSpeedColumn;
    private final String solarColumn;
    private final String rainfallColumn;

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

    public String getTimestampColumn() {
        return timestampColumn;
    }

    public String getTemperatureColumn() {
        return temperatureColumn;
    }

    public String getHumidityColumn() {
        return humidityColumn;
    }

    public String getWindSpeedColumn() {
        return windSpeedColumn;
    }

    public String getSolarColumn() {
        return solarColumn;
    }

    public String getRainfallColumn() {
        return rainfallColumn;
    }
}