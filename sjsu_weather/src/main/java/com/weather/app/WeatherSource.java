package com.weather.app;
import java.util.List;

public interface WeatherSource {
    List<WeatherRecord> fetchAll(String lastTimestamp);
    String[] getHeaders();
}

