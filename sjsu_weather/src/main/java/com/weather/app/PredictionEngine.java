package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
    PredictionEngine | Jayden's Note:
    Responsibility: Implement the 5 days temperature forecast by coordinating the three specialized components.

    Delegates to:
    - TemperatureAggregator -> groups raw data into daily averages
    - LinearTrendCalculator -> computes baseline and trend slope
    - ConfidenceEvaluator -> assigns confidence label per forecast day

    This class contains no math, that's for the delegation above, this class only coordinates.
    Might swap in ML later, easy to replace this class while keeping the others.
*/

public class PredictionEngine {

    //  Number of days to forecast ahead
    private final int predictionHorizon;

    private final TemperatureAggregator aggregator;
    private final LinearTrendCalculator trendCalculator;
    private final ConfidenceEvaluator confidenceEvaluator;

    //  Contructors

    //  Default constructor

    public PredictionEngine() {
        this(5);
    }

    //  Constructor with configurable forecast horizon.
    //  @param predictionHorizon number of days to forecast

    public PredictionEngine(int predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
        this.aggregator = new TemperatureAggregator();
        this.trendCalculator = new LinearTrendCalculator();
        this.confidenceEvaluator = new ConfidenceEvaluator();
    }

    //  Public API
    //  Generates a multi-day temperature forecast from historical WeatherData.

    /*  
        Steps:
        1. Aggregate raw records into daily averages
        2. Compute weighted moving average (baseline)
        3. Compute linear trend (slope per day)
        4. Compute standard deviation (for confidence)
        5. Project baseline + trend for each forecast day

        @param historicalData list of past WeatherData records
        @return list of ForecastEntry, one per forecast day
    */

    public List<ForecastEntry> generateForecast(List<WeatherData> historicalData) {
        List<ForecastEntry> forecast = new ArrayList<>();

        if (historicalData == null || historicalData.isEmpty()) {
            return forecast;
        }

        // Step 1: Aggregate into daily averages
        List<Double> dailyAverages = aggregator.computeDailyAverages(historicalData);
        if (dailyAverages.isEmpty()) return forecast;

        // Step 2 & 3: Baseline and trend
        double baseline = trendCalculator.computeWeightedMovingAverage(dailyAverages);
        double trend = trendCalculator.computeLinearTrend(dailyAverages);

        // Step 4: Standard deviation for confidence scoring
        double stdDev = confidenceEvaluator.computeStandardDeviation(dailyAverages);

        // Step 5: Build one ForecastEntry per day
        for (int i = 1; i <= predictionHorizon; i++) {
            LocalDate forecastDate = LocalDate.now().plusDays(i);
            double predictedTemp = baseline + (trend * i);
            String confidence = confidenceEvaluator.evaluate(stdDev, i);

            forecast.add(new ForecastEntry(forecastDate, predictedTemp, confidence));
        }

        return forecast;
    }
}