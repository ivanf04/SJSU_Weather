package com.weather.app;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A small reusable UI component used to display ONE weather value.
 *
 * Examples:
 * - Temperature: 72°F
 * - Humidity: 45%
 * - Wind: 8 mph
 *
 * WeatherDashboard creates several ValueCards for current weather metrics.
 */
public class ValueCard extends VBox {

    /**
     * This label holds the actual value (e.g., "72°F").
     *
     * It changes when WeatherDashboard receives new data.
     */
    private final Label valueLabel;

    /**
     * Constructor creates the card UI.
     *
     * @param title The label shown at the top (e.g., "Temperature")
     * @param initialValue The starting value (usually "--" before data loads)
     */
    public ValueCard(String title, String initialValue) {

        // Spacing between title and value.
        setSpacing(6);

        // Padding around the inside of the card.
        setPadding(new Insets(12));

        // Basic border and rounded background styling.
        setStyle("-fx-border-color: #d8d8d8; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title label identifies what metric this card displays.
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // Value label displays the current metric value.
        valueLabel = new Label(initialValue);
        valueLabel.setStyle("-fx-font-size: 20;");

        // Add both labels into this VBox component.
        getChildren().addAll(titleLabel, valueLabel);
    }

    /**
     * Updates the value shown in the card.
     *
     * This is called from WeatherDashboard when new data arrives.
     */
    public void setValue(String value) {
        valueLabel.setText(value);
    }
}