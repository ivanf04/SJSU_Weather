package com.weather.app;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 *
 * A small reusable UI component used to display ONE weather value.
 * Examples: temperature, humidity, wind speed, rainfall, etc.
 * 
 */
public class ValueCard extends VBox {

    /**
     * This label holds the actual value (e.g., "72F").
     */
    private final Label valueLabel;

    /**
     * Constructor creates the card UI.
     *
     * @param title The label shown at the top (e.g., "Temperature")
     * @param initialValue The starting value (usually "--" before data loads)
     */
    public ValueCard(String title, String initialValue) {

        setSpacing(6);
        setPadding(new Insets(12));
        setStyle("-fx-border-color: #d8d8d8; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        valueLabel = new Label(initialValue);
        valueLabel.setStyle("-fx-font-size: 20;");

        getChildren().addAll(titleLabel, valueLabel);
    }

    /**
     * Updates the value shown in the card. 
     * This is called from WeatherDashboard when new data arrives.
     *
     */
    public void setValue(String value) {
        valueLabel.setText(value);
    }
}