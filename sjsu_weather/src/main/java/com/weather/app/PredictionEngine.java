package com.weather.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default forecast model.
 *
 * Uses:
 * - TemperatureAggregator for daily averages
 * - LinearTrendCalculator for baseline and trend
 * - ConfidenceEvaluator for confidence labels
 */
public class PredictionEngine implements ForecastModel {

    private final int predictionHorizon;
    private final TemperatureAggregator aggregator;
    private final LinearTrendCalculator trendCalculator;
    private final ConfidenceEvaluator confidenceEvaluator;

    public PredictionEngine() {
        this(5);
    }

    public PredictionEngine(int predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
        this.aggregator = new TemperatureAggregator();
        this.trendCalculator = new LinearTrendCalculator();
        this.confidenceEvaluator = new ConfidenceEvaluator();
    }

    @Override
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

        List<Double> recent = dailyAverages.subList(
                Math.max(0, dailyAverages.size() - 5),
                dailyAverages.size()
        );

        double stdDev = confidenceEvaluator.computeStandardDeviation(recent);

        for (int day = 1; day <= predictionHorizon; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            double predictedTemp = baseline + (trend * day);
            String confidence = confidenceEvaluator.evaluate(stdDev, trend, day);

            forecast.add(new ForecastEntry(date, predictedTemp, confidence));
        }

        return forecast;
    }
}