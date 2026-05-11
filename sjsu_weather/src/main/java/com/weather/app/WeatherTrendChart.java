package com.weather.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Simple custom chart for plotting a selected WeatherData metric.
 *
 * Default metric is temperature.
 */
public class WeatherTrendChart extends StackPane {

    private final Canvas canvas;
    private List<WeatherData> data = new ArrayList<>();
    private WeatherMetric metric = WeatherMetric.TEMPERATURE;

    public WeatherTrendChart(double width, double height) {
        canvas = new Canvas(width, height);
        getChildren().add(canvas);
    }

    public void setData(List<WeatherData> data) {
        this.data = data == null ? new ArrayList<>() : data;
        redraw();
    }

    public void setMetric(WeatherMetric metric) {
        this.metric = metric == null ? WeatherMetric.TEMPERATURE : metric;
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        gc.clearRect(0, 0, width, height);

        if (data == null || data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("No trend data", 20, 20);
            return;
        }

        double min = data.stream()
                .map(this::getMetricValue)
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        double max = data.stream()
                .map(this::getMetricValue)
                .max(Comparator.naturalOrder())
                .orElse(1.0);

        if (Math.abs(max - min) < 0.001) {
            max = min + 1.0;
        }

        double left = 40;
        double right = 20;
        double top = 20;
        double bottom = 30;
        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;

        gc.setStroke(Color.GRAY);
        gc.strokeLine(left, height - bottom, width - right, height - bottom);
        gc.strokeLine(left, top, left, height - bottom);

        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.1f", max), 5, top + 5);
        gc.fillText(String.format("%.1f", min), 5, height - bottom);
        gc.fillText(getMetricLabel(), left + 5, top + 15);

        gc.setStroke(Color.BLUE);
        gc.setFill(Color.BLUE);

        for (int i = 0; i < data.size(); i++) {
            double value = getMetricValue(data.get(i));

            double x = left + (plotWidth * i / Math.max(1, data.size() - 1));
            double y = top + plotHeight - ((value - min) / (max - min)) * plotHeight;

            gc.fillOval(x - 2, y - 2, 4, 4);

            if (i > 0) {
                double previousValue = getMetricValue(data.get(i - 1));
                double previousX = left + (plotWidth * (i - 1) / Math.max(1, data.size() - 1));
                double previousY = top + plotHeight - ((previousValue - min) / (max - min)) * plotHeight;

                gc.strokeLine(previousX, previousY, x, y);
            }
        }
    }

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