package com.weather.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Custom chart component for displaying weather trends.
 *
 * This class uses a JavaFX Canvas to manually draw a line chart.
 *
 * Key features:
 * - Plots a list of WeatherData points
 * - Supports multiple metrics (temperature, humidity, etc.)
 *
 * This is a reusable UI component that can be used for different weather metrics.
 */
public class WeatherTrendChart extends StackPane {

    /**
     * Canvas used for drawing the chart.
     */
    private final Canvas canvas;

    /**
     * Data points to be plotted.
     */
    private List<WeatherData> data = new ArrayList<>();

    /**
     * Metric to plot (temperature by default).
     */
    private WeatherMetric metric = WeatherMetric.TEMPERATURE;

    /**
     * Creates a new chart with given width and height.
     */
    public WeatherTrendChart(double width, double height) {
        canvas = new Canvas(width, height);

        // Add canvas to this container
        getChildren().add(canvas);
    }

    /**
     * Sets new data for the chart and redraws it.
     */
    public void setData(List<WeatherData> data) {
        this.data = data == null ? new ArrayList<>() : data;
        redraw();
    }

    /**
     * Sets which metric should be displayed.
     *
     * Example:
     * - TEMPERATURE
     * - HUMIDITY
     */
    public void setMetric(WeatherMetric metric) {
        this.metric = metric == null ? WeatherMetric.TEMPERATURE : metric;
        redraw();
    }

    /**
     * Redraws the entire chart.
     *
     * This method:
     * - clears canvas
     * - calculates min/max
     * - draws axes
     * - plots data points
     * - connects points with lines
     */
    private void redraw() {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // Clear previous drawing
        gc.clearRect(0, 0, width, height);

        // If no data, display message
        if (data == null || data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("No trend data", 20, 20);
            return;
        }

        // Find min and max values for scaling
        double min = data.stream()
                .map(this::getMetricValue)
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        double max = data.stream()
                .map(this::getMetricValue)
                .max(Comparator.naturalOrder())
                .orElse(1.0);

        // Avoid division by zero if all values are equal
        if (Math.abs(max - min) < 0.001) {
            max = min + 1.0;
        }

        // Padding for axes
        double left = 40;
        double right = 20;
        double top = 20;
        double bottom = 30;

        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;

        // Draw axes
        gc.setStroke(Color.GRAY);
        gc.strokeLine(left, height - bottom, width - right, height - bottom); // x-axis
        gc.strokeLine(left, top, left, height - bottom); // y-axis

        // Draw labels
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.1f", max), 5, top + 5);
        gc.fillText(String.format("%.1f", min), 5, height - bottom);
        gc.fillText(getMetricLabel(), left + 5, top + 15);

        // Draw data points and lines
        gc.setStroke(Color.BLUE);
        gc.setFill(Color.BLUE);

        for (int i = 0; i < data.size(); i++) {

            double value = getMetricValue(data.get(i));

            // Convert data index → x coordinate
            double x = left + (plotWidth * i / Math.max(1, data.size() - 1));

            // Convert value → y coordinate
            double y = top + plotHeight - ((value - min) / (max - min)) * plotHeight;

            // Draw point
            gc.fillOval(x - 2, y - 2, 4, 4);

            // Draw line from previous point
            if (i > 0) {
                double previousValue = getMetricValue(data.get(i - 1));

                double previousX = left + (plotWidth * (i - 1) / Math.max(1, data.size() - 1));
                double previousY = top + plotHeight - ((previousValue - min) / (max - min)) * plotHeight;

                gc.strokeLine(previousX, previousY, x, y);
            }
        }
    }

    /**
     * Returns the correct value based on selected metric.
     */
    private double getMetricValue(WeatherData point) {
        switch (metric) {
            case HUMIDITY:
                return point.getHumidity();
            case WIND_SPEED:
                return point.getWindSpeed();
            case SOLAR:
                return point.getSolarIrradiance();
            case RAINFALL:
                return point.getRainfall();
            case TEMPERATURE:
            default:
                return point.getTemperature();
        }
    }

    /**
     * Returns label text for the selected metric.
     */
    private String getMetricLabel() {
        switch (metric) {
            case HUMIDITY:
                return "Humidity (%)";
            case WIND_SPEED:
                return "Wind (mph)";
            case SOLAR:
                return "Solar (W/m²)";
            case RAINFALL:
                return "Rainfall (in)";
            case TEMPERATURE:
            default:
                return "Temperature (°F)";
        }
    }
}