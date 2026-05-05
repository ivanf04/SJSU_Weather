package com.weather.app;

import java.util.LinkedHashMap;
import java.util.Map;

public class WeatherRecord {
    private final Map<String, String> data = new LinkedHashMap<>();

    public void setValue(String key, String value) { data.put(key, value); }
    public String getValue(String key) { return data.getOrDefault(key, ""); }
}