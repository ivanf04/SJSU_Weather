package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default forecast model.
 *
 * This class implements ForecastModel, so it is one interchangeable
 * forecasting strategy.
 *
 * Forecast process:
 * 1. Convert raw WeatherData records into daily average temperatures.
 * 2. Compute a weighted moving average to get a baseline temperature.
 * 3. Compute a linear trend to estimate warming/cooling direction.
 * 4. Use recent temperature variation to calculate confidence.
 * 5. Generate one ForecastEntry per future day.
 *
 * Uses:
 * - TemperatureAggregator for daily averages
 * - LinearTrendCalculator for baseline and trend
 * - ConfidenceEvaluator for confidence labels
 */
public class PredictionEngine implements ForecastModel {

    /**
     * Number of days to forecast.
     *
     * Default constructor uses 5 days.
     */
    private final int predictionHorizon;

    /**
     * Converts WeatherData records into daily average temperatures.
     */
    private final TemperatureAggregator aggregator;

    /**
     * Performs weighted average and trend calculations.
     */
    private final LinearTrendCalculator trendCalculator;

    /**
     * Assigns High/Medium/Low confidence labels.
     */
    private final ConfidenceEvaluator confidenceEvaluator;

    /**
     * Default prediction engine forecasts 5 days.
     */
    public PredictionEngine() {
        this(5);
    }

    /**
     * Constructor that allows tests or future features to use a custom horizon.
     *
     * @param predictionHorizon number of days to forecast
     */
    public PredictionEngine(int predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
        this.aggregator = new TemperatureAggregator();
        this.trendCalculator = new LinearTrendCalculator();
        this.confidenceEvaluator = new ConfidenceEvaluator();
    }

    /**
     * Generates a list of future forecast entries from historical data.
     *
     * The forecast is based on historical daily average temperatures.
     */
    @Override
    public List<ForecastEntry> generateForecast(List<WeatherData> historicalData) {

        // Forecast list starts empty and is returned empty if there is not enough data.
        List<ForecastEntry> forecast = new ArrayList<>();

        // No historical data means there is nothing to forecast from.
        if (historicalData == null || historicalData.isEmpty()) {
            return forecast;
        }

        // Step 1: Convert many raw weather readings into one average per day.
        List<Double> dailyAverages = aggregator.computeDailyAverages(historicalData);

        // If all records were invalid/skipped, stop safely.
        if (dailyAverages.isEmpty()) {
            return forecast;
        }

        // Step 2: Baseline temperature uses recent data with more weight on newer days.
        double baseline = trendCalculator.computeWeightedMovingAverage(dailyAverages);

        // Step 3: Trend estimates temperature change per day.
        // Positive trend = warming, negative trend = cooling.
        double trend = trendCalculator.computeLinearTrend(dailyAverages);

        // Use only the most recent 5 daily averages for confidence scoring.
        // Recent consistency is more relevant to short-term confidence.
        List<Double> recent = dailyAverages.subList(
                Math.max(0, dailyAverages.size() - 5),
                dailyAverages.size()
        );

        // Standard deviation measures how spread out recent daily averages are.
        // Lower stdDev means more stable temperatures and usually higher confidence.
        double stdDev = confidenceEvaluator.computeStandardDeviation(recent);

        // Step 4: Build one ForecastEntry for each future day.
        for (int day = 1; day <= predictionHorizon; day++) {

            // Forecast date is day(s) after today.
            LocalDate date = LocalDate.now().plusDays(day);

            // Prediction = recent weighted baseline + trend projected forward.
            double predictedTemp = baseline + (trend * day);

            // Confidence depends on variation, trend strength, and days ahead.
            String confidence = confidenceEvaluator.evaluate(stdDev, trend, day);

            // Add one row for the forecast table/cache.
            forecast.add(new ForecastEntry(date, predictedTemp, confidence));
        }

        return forecast;
    }
}