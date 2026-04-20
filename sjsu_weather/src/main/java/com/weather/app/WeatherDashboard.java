package com.weather.app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 *
 * Main JavaFX UI for the application.
 *
 * Responsibilities:
 * - Display current weather values
 * - Display historical data table
 * - Display daily and weekly trend graphs
 * - Display forecast results
 * - Show system status (live, cached, stale, error)
 *
 * Design notes:
 * This class is UI-only.
 * It does NOT handle scraping, CSV parsing, or data creation.
 * It depends on DashboardDataProvider for all data.
 */
public class WeatherDashboard extends Application {

    /**
     * Interface used by the UI to request data.
     */
    private DashboardDataProvider dataProvider;

    /* ---------- Header/Status ---------- */

    /** Displays current system state (LIVE, ERROR, etc.) */
    private Label statusLabel;

    /** Displays timestamp of current data */
    private Label timestampLabel;

    /** Displays daily high/low summary */
    private Label dailyHighLowLabel;

    /* ---------- Current weather cards ---------- */

    private ValueCard temperatureCard;
    private ValueCard feelsLikeCard;
    private ValueCard humidityCard;
    private ValueCard windCard;
    private ValueCard solarCard;
    private ValueCard rainfallCard;

    /* ---------- Buttons ---------- */

    private Button refreshButton;
    private Button retryButton;
    private Button loadHistoryButton;
    private Button refreshForecastButton;

    /* ---------- Date selection ---------- */

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    /* ---------- Tables ---------- */

    private TableView<WeatherData> historyTable;
    private ObservableList<WeatherData> historyItems;

    private TableView<ForecastEntry> forecastTable;
    private ObservableList<ForecastEntry> forecastItems;

    /* ---------- Charts ---------- */

    private WeatherTrendChart dailyTrendChart;
    private WeatherTrendChart weeklyTrendChart;

    /** Formatter used for displaying dates */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Entry point for JavaFX app.
     */
    @Override
    public void start(Stage stage) {

        // TODO: Replace with real implementation
        dataProvider = null;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        root.setTop(buildTopSection());
        root.setCenter(buildCenterSection());

        initializeDefaults();

        Scene scene = new Scene(root, 1500, 940);
        stage.setTitle("SJSU Weather Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Builds top section (status + current weather cards).
     */
    private VBox buildTopSection() {
        VBox root = new VBox(12);

        // Status bar
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(12));

        statusLabel = new Label("Status: Waiting for data");
        statusLabel.setFont(Font.font(15));

        timestampLabel = new Label("Last updated: --");

        refreshButton = new Button("Refresh");
        retryButton = new Button("Retry");

        refreshButton.setOnAction(e -> refreshLiveWeather());
        retryButton.setOnAction(e -> handleRetry());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, timestampLabel, spacer, refreshButton, retryButton);

        // Weather cards
        GridPane cards = new GridPane();
        cards.setHgap(10);
        cards.setVgap(10);

        temperatureCard = new ValueCard("Temperature", "--");
        feelsLikeCard = new ValueCard("Feels Like", "--");
        humidityCard = new ValueCard("Humidity", "--");
        windCard = new ValueCard("Wind", "--");
        solarCard = new ValueCard("Solar", "--");
        rainfallCard = new ValueCard("Rainfall", "--");

        cards.add(temperatureCard, 0, 0);
        cards.add(feelsLikeCard, 1, 0);
        cards.add(humidityCard, 2, 0);
        cards.add(windCard, 0, 1);
        cards.add(solarCard, 1, 1);
        cards.add(rainfallCard, 2, 1);

        root.getChildren().addAll(statusBar, cards);
        return root;
    }

    /**
     * Builds main center layout (split pane).
     */
    private SplitPane buildCenterSection() {
        SplitPane split = new SplitPane();
        split.getItems().addAll(buildHistoryPane(), buildAnalysisPane());
        return split;
    }

    /**
     * Left side: historical table + date range.
     */
    private VBox buildHistoryPane() {
        VBox root = new VBox(10);

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();

        loadHistoryButton = new Button("Load History");
        loadHistoryButton.setOnAction(e -> loadHistory(
                startDatePicker.getValue(),
                endDatePicker.getValue()
        ));

        historyTable = new TableView<>();
        historyItems = FXCollections.observableArrayList();
        historyTable.setItems(historyItems);

        root.getChildren().addAll(startDatePicker, endDatePicker, loadHistoryButton, historyTable);
        return root;
    }

    /**
     * Right side: charts + forecast.
     */
    private VBox buildAnalysisPane() {
        VBox root = new VBox(10);

        dailyTrendChart = new WeatherTrendChart(600, 200);
        weeklyTrendChart = new WeatherTrendChart(600, 200);

        dailyHighLowLabel = new Label("Daily High/Low: --");

        forecastTable = new TableView<>();
        forecastItems = FXCollections.observableArrayList();
        forecastTable.setItems(forecastItems);

        root.getChildren().addAll(
                dailyTrendChart,
                weeklyTrendChart,
                dailyHighLowLabel,
                forecastTable
        );

        return root;
    }

    /**
     * Sets default date range.
     */
    private void initializeDefaults() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(5));
        endDatePicker.setValue(today);
    }

    /**
     * Loads current weather asynchronously.
     */
    public void refreshLiveWeather() {
        if (dataProvider == null) return;

        Task<WeatherData> task = new Task<>() {
            protected WeatherData call() {
                return dataProvider.getCurrentWeather();
            }
        };

        task.setOnSucceeded(e -> updateCurrentWeather(task.getValue()));

        new Thread(task).start();
    }

    /**
     * Loads historical data.
     */
    public void loadHistory(LocalDate start, LocalDate end) {
        if (dataProvider == null) return;

        Task<List<WeatherData>> task = new Task<>() {
            protected List<WeatherData> call() {
                return dataProvider.getHistoricalWeather(start, end);
            }
        };

        task.setOnSucceeded(e -> historyItems.setAll(task.getValue()));

        new Thread(task).start();
    }

    /**
     * Loads forecast data.
     */
    public void loadForecast() {
        if (dataProvider == null) return;

        Task<List<ForecastEntry>> task = new Task<>() {
            protected List<ForecastEntry> call() {
                return dataProvider.getForecast();
            }
        };

        task.setOnSucceeded(e -> forecastItems.setAll(task.getValue()));

        new Thread(task).start();
    }

    /**
     * Updates UI with current weather.
     */
    public void updateCurrentWeather(WeatherData data) {
        if (data == null) return;

        temperatureCard.setValue(format(data.getTemperature()));
        feelsLikeCard.setValue(format(data.getFeelsLike()));
        humidityCard.setValue(format(data.getHumidity()));
        windCard.setValue(format(data.getWindSpeed()));
        solarCard.setValue(format(data.getSolarIrradiance()));
        rainfallCard.setValue(format(data.getRainfall()));

        timestampLabel.setText("Last updated: " + data.getFormattedTimestamp());
    }

    public void handleRetry() {
        refreshLiveWeather();
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Enum for UI state.
     */
    public enum SystemStatus {
        LIVE, CACHED, STALE, ERROR, LOADING
    }
}