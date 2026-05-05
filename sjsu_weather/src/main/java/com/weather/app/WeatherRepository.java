package com.weather.app;
import java.util.List;

public interface WeatherRepository {
    String getFirstTimestamp();
    String getLastTimestamp();
    void append(List<WeatherRecord> records, String[] headers);
}