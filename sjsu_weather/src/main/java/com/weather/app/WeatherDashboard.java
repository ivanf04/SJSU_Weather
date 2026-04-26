package com.weather.app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
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
        dataProvider = new LocalCsvDataProvider("sjsu_weather_backup.csv");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        root.setTop(buildTopSection());
        root.setCenter(buildCenterSection());

        initializeDefaults();

        Scene scene = new Scene(root, 1500, 940);
        stage.setTitle("SJSU Weather Dashboard");
        stage.setScene(scene);
        stage.show();

        // Load UI data immediately after the window is shown
        refreshLiveWeather();
        loadHistory(startDatePicker.getValue(), endDatePicker.getValue());
        loadForecast();
        loadTrendViews();
    }

    /**
     * Builds top section (status + current weather cards).
     */
    private VBox buildTopSection() {
        VBox root = new VBox(12);

        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(12));

        statusLabel = new Label("Status: Waiting for data");
        statusLabel.setFont(Font.font(15));

        timestampLabel = new Label("Last updated: --");

        refreshButton = new Button("Refresh");
        retryButton = new Button("Retry");

        refreshButton.setOnAction(e -> {
            refreshLiveWeather();
            loadTrendViews();
            loadForecast();
        });
        retryButton.setOnAction(e -> handleRetry());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, timestampLabel, spacer, refreshButton, retryButton);

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
        split.setDividerPositions(0.52);
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
        loadHistoryButton.setOnAction(e -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start == null || end == null) {
                showError("Please select both a start date and end date.");
                return;
            }

            if (end.isBefore(start)) {
                showError("End date must be on or after the start date.");
                return;
            }

            loadHistory(start, end);
        });

        historyTable = new TableView<>();
        historyItems = FXCollections.observableArrayList();
        historyTable.setItems(historyItems);
        configureHistoryTable();

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

        refreshForecastButton = new Button("Refresh Forecast");
        refreshForecastButton.setOnAction(e -> loadForecast());

        forecastTable = new TableView<>();
        forecastItems = FXCollections.observableArrayList();
        forecastTable.setItems(forecastItems);
        configureForecastTable();

        root.getChildren().addAll(
                new Label("Daily Temperature Trend"),
                dailyTrendChart,
                new Label("Weekly Temperature Trend"),
                weeklyTrendChart,
                dailyHighLowLabel,
                refreshForecastButton,
                forecastTable
        );

        return root;
    }

    private void configureHistoryTable() {
        TableColumn<WeatherData, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFormattedTimestamp()));

        TableColumn<WeatherData, String> tempCol = new TableColumn<>("Temp");
        tempCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getTemperature())));

        TableColumn<WeatherData, String> feelsLikeCol = new TableColumn<>("Feels Like");
        feelsLikeCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getFeelsLike())));

        TableColumn<WeatherData, String> humidityCol = new TableColumn<>("Humidity");
        humidityCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getHumidity())));

        TableColumn<WeatherData, String> windCol = new TableColumn<>("Wind");
        windCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getWindSpeed())));

        TableColumn<WeatherData, String> solarCol = new TableColumn<>("Solar");
        solarCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getSolarIrradiance())));

        TableColumn<WeatherData, String> rainCol = new TableColumn<>("Rainfall");
        rainCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getRainfall())));

        historyTable.getColumns().setAll(
                timestampCol, tempCol, feelsLikeCol, humidityCol, windCol, solarCol, rainCol
        );
    }

    private void configureForecastTable() {
        TableColumn<ForecastEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(dateFormatter)));

        TableColumn<ForecastEntry, String> tempCol = new TableColumn<>("Predicted Temp");
        tempCol.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%.2f", cell.getValue().getPredictedTemperature())));

        TableColumn<ForecastEntry, String> confidenceCol = new TableColumn<>("Confidence");
        confidenceCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getConfidenceLabel()));

        forecastTable.getColumns().setAll(dateCol, tempCol, confidenceCol);
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

        setStatus(SystemStatus.LOADING, "Loading current weather...");

        Task<WeatherData> task = new Task<WeatherData>() {
            @Override
            protected WeatherData call() {
                return dataProvider.getCurrentWeather();
            }
        };

        task.setOnSucceeded(e -> updateCurrentWeather(task.getValue()));
        task.setOnFailed(e -> {
            setStatus(SystemStatus.ERROR, "Failed to load current weather");
            showError(task.getException() == null ? "Unknown error loading weather"
                    : task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Loads historical data.
     */
    public void loadHistory(LocalDate start, LocalDate end) {
        if (dataProvider == null) return;

        Task<List<WeatherData>> task = new Task<List<WeatherData>>() {
            @Override
            protected List<WeatherData> call() {
                return dataProvider.getHistoricalWeather(start, end);
            }
        };

        task.setOnSucceeded(e -> historyItems.setAll(task.getValue()));
        task.setOnFailed(e -> showError(task.getException() == null ? "Unknown error loading history"
                : task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Loads forecast data.
     */
    public void loadForecast() {
        if (dataProvider == null) return;

        Task<List<ForecastEntry>> task = new Task<List<ForecastEntry>>() {
            @Override
            protected List<ForecastEntry> call() {
                return dataProvider.getForecast();
            }
        };

        task.setOnSucceeded(e -> forecastItems.setAll(task.getValue()));
        task.setOnFailed(e -> showError(task.getException() == null ? "Unknown error loading forecast"
                : task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Loads daily/weekly chart data and daily summary.
     */
    public void loadTrendViews() {
        if (dataProvider == null) return;

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                List<WeatherData> daily = dataProvider.getDailyTrend();
                List<WeatherData> weekly = dataProvider.getWeeklyTrend();
                DailySummary summary = dataProvider.getDailySummary();

                Platform.runLater(() -> {
                    dailyTrendChart.setData(daily);
                    weeklyTrendChart.setData(weekly);

                    if (summary != null) {
                        dailyHighLowLabel.setText(String.format(
                                "Daily High/Low: %.2f / %.2f",
                                summary.getHighTemp(),
                                summary.getLowTemp()
                        ));
                    } else {
                        dailyHighLowLabel.setText("Daily High/Low: --");
                    }
                });

                return null;
            }
        };

        task.setOnFailed(e -> showError(task.getException() == null ? "Unknown error loading trends"
                : task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates UI with current weather.
     */
    public void updateCurrentWeather(WeatherData data) {
        if (data == null) {
            setStatus(SystemStatus.ERROR, "No weather data available");
            return;
        }

        temperatureCard.setValue(format(data.getTemperature()));
        feelsLikeCard.setValue(format(data.getFeelsLike()));
        humidityCard.setValue(format(data.getHumidity()));
        windCard.setValue(format(data.getWindSpeed()));
        solarCard.setValue(format(data.getSolarIrradiance()));
        rainfallCard.setValue(format(data.getRainfall()));

        timestampLabel.setText("Last updated: " + data.getFormattedTimestamp());

        SystemStatus status = data.getStatus();
        if (status == null) status = SystemStatus.LIVE;

        switch (status) {
            case STALE:
                setStatus(SystemStatus.STALE, "Data is stale");
                break;
            case CACHED:
                setStatus(SystemStatus.CACHED, "Showing cached data");
                break;
            case ERROR:
                setStatus(SystemStatus.ERROR, "Data error");
                break;
            case LOADING:
                setStatus(SystemStatus.LOADING, "Loading");
                break;
            case LIVE:
            default:
                setStatus(SystemStatus.LIVE, "Live data loaded");
                break;
        }
    }

    public void handleRetry() {
        refreshLiveWeather();
        loadHistory(startDatePicker.getValue(), endDatePicker.getValue());
        loadForecast();
        loadTrendViews();
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    private void setStatus(SystemStatus status, String message) {
        statusLabel.setText("Status: " + message);

        String style = "-fx-padding: 6 10 6 10; -fx-background-radius: 6;";
        switch (status) {
            case LIVE:
                style += "-fx-background-color: #d4edda; -fx-text-fill: #155724;";
                break;
            case CACHED:
                style += "-fx-background-color: #fff3cd; -fx-text-fill: #856404;";
                break;
            case STALE:
                style += "-fx-background-color: #ffe5b4; -fx-text-fill: #8a4b00;";
                break;
            case ERROR:
                style += "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
                break;
            case LOADING:
            default:
                style += "-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460;";
                break;
        }
        statusLabel.setStyle(style);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Weather Dashboard Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Enum for UI state.
     */
    public enum SystemStatus {
        LIVE, CACHED, STALE, ERROR, LOADING
    }
}