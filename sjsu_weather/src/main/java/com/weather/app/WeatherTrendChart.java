package com.weather.app;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

/**
 * Simple custom chart for plotting temperature trends from WeatherData.
 * TO DO (only has placeholder methods)
 */
public class WeatherTrendChart extends StackPane {
    private final Canvas canvas;
    private List<WeatherData> data = new ArrayList<>();

    public WeatherTrendChart(double width, double height) {
        canvas = new Canvas(width, height);
        getChildren().add(canvas);
    }

    public void setData(List<WeatherData> data) {
        // TO DO
        this.data = data;
        redraw();
    }

    private void redraw() {
        // TO DO
    }
}