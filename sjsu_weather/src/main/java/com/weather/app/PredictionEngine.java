package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a multi-day temperature forecast from historical WeatherData.
 *
 * This class coordinates:
 * - TemperatureAggregator for daily averages
 * - LinearTrendCalculator for baseline and slope
 * - ConfidenceEvaluator for confidence labels
 */
public class PredictionEngine {

    private final int predictionHorizon;
    private final TemperatureAggregator aggregator;
    private final LinearTrendCalculator trendCalculator;
    private final ConfidenceEvaluator confidenceEvaluator;

    /**
     * Default forecast horizon = 5 days.
     */
    public PredictionEngine() {
        this(5);
    }

    /**
     * Creates a prediction engine with a configurable horizon.
     */
    public PredictionEngine(int predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
        this.aggregator = new TemperatureAggregator();
        this.trendCalculator = new LinearTrendCalculator();
        this.confidenceEvaluator = new ConfidenceEvaluator();
    }

    /**
     * Generates forecast entries from historical weather data.
     */
    public List<ForecastEntry> generateForecast(List<WeatherData> historicalData) {
        List<ForecastEntry> forecast = new ArrayList<>();

        if (historicalData == null || historicalData.isEmpty()) {
            return forecast;
        }

        List<Double> dailyAverages = aggregator.computeDailyAverages(historicalData);
        if (dailyAverages.isEmpty()) {
            return forecast;
        }

        double baseline = trendCalculator.computeWeightedMovingAverage(dailyAverages);
        double trend = trendCalculator.computeLinearTrend(dailyAverages);
        double stdDev = confidenceEvaluator.computeStandardDeviation(dailyAverages);

        for (int day = 1; day <= predictionHorizon; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            double predictedTemp = baseline + (trend * day);
            String confidence = confidenceEvaluator.evaluate(stdDev, day);

            forecast.add(new ForecastEntry(date, predictedTemp, confidence));
        }

        return forecast;
    }
}