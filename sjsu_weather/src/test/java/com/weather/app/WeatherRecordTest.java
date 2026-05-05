package com.weather.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * WeatherRecordTest
 *
 * WeatherRecord is a thin key-value store. We test:
 *   1. A value that was set can be retrieved
 *   2. Getting a key that was never set returns "" not null (the default)
 *   3. Setting the same key twice overwrites the first value
 *   4. Keys are case-sensitive (TIMESTAMP != timestamp)
 */
class WeatherRecordTest {

    @Test
    void getValue_returnsSetValue() {
        WeatherRecord record = new WeatherRecord();
        record.setValue("TIMESTAMP", "2026-04-22 10:00");
        assertEquals("2026-04-22 10:00", record.getValue("TIMESTAMP"));
    }

    @Test
    void getValue_returnsEmptyString_forMissingKey() {
        WeatherRecord record = new WeatherRecord();
        // Never set "TEMPERATURE" — should return "" not null
        String result = record.getValue("TEMPERATURE");
        assertNotNull(result, "getValue should never return null");
        assertEquals("", result, "Missing key should return empty string");
    }

    @Test
    void setValue_overwritesPreviousValue() {
        WeatherRecord record = new WeatherRecord();
        record.setValue("TEMP", "15.0");
        record.setValue("TEMP", "18.5");
        assertEquals("18.5", record.getValue("TEMP"),
                "Second setValue should overwrite the first");
    }

    @Test
    void getValue_isCaseSensitive() {
        WeatherRecord record = new WeatherRecord();
        record.setValue("TIMESTAMP", "2026-04-22");
        // "timestamp" (lowercase) is a different key
        assertEquals("", record.getValue("timestamp"),
                "Key lookup should be case-sensitive");
    }
}
