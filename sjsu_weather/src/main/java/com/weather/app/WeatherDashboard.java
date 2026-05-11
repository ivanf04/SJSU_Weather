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
 * JavaFX dashboard for the SJSU Weather app.
 *
 * This class is UI-only. It gets all data through DashboardDataProvider.
 */
public class WeatherDashboard extends Application {

    private static volatile DashboardDataProvider injectedDataProvider;

    public static void setInjectedDataProvider(DashboardDataProvider provider) {
        injectedDataProvider = provider;
    }

    private DashboardDataProvider dataProvider;

    private Label statusLabel;
    private Label timestampLabel;
    private Label dailyHighLowLabel;

    private ValueCard temperatureCard;
    private ValueCard feelsLikeCard;
    private ValueCard humidityCard;
    private ValueCard windCard;
    private ValueCard solarCard;
    private ValueCard rainfallCard;

    private Button loadHistoryButton;
    private Button refreshForecastButton;

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private TableView<WeatherData> historyTable;
    private ObservableList<WeatherData> historyItems;

    private TableView<ForecastEntry> forecastTable;
    private ObservableList<ForecastEntry> forecastItems;

    private WeatherTrendChart dailyTrendChart;
    private WeatherTrendChart weeklyTrendChart;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void start(Stage stage) {
        dataProvider = resolveDataProvider();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildTopSection());
        root.setCenter(buildCenterSection());

        initializeDefaults();

        Scene scene = new Scene(root, 1500, 940);
        stage.setTitle("SJSU Weather Dashboard");
        stage.setScene(scene);
        stage.show();

        refreshAll();
    }

    private DashboardDataProvider resolveDataProvider() {
        DashboardDataProvider injected = injectedDataProvider;
        injectedDataProvider = null;

        if (injected != null) {
            return injected;
        }

        WeatherAppComposition composition = WeatherAppComposition.createDefault();
        System.out.println("Dashboard using CSV file: " + composition.getCsvPath());
        return composition.createDashboardDataProvider();
    }

    private VBox buildTopSection() {
        VBox root = new VBox(12);

        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(12));

        statusLabel = new Label("Status: Waiting for data");
        statusLabel.setFont(Font.font(15));

        timestampLabel = new Label("Last updated: --");

        Button refreshButton = new Button("Refresh");
        Button retryButton = new Button("Retry");

        refreshButton.setOnAction(e -> refreshAll());
        retryButton.setOnAction(e -> refreshAll());

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

    private SplitPane buildCenterSection() {
        SplitPane split = new SplitPane();
        split.getItems().addAll(buildHistoryPane(), buildAnalysisPane());
        split.setDividerPositions(0.52);
        return split;
    }

    private VBox buildHistoryPane() {
        VBox root = new VBox(10);

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();

        loadHistoryButton = new Button("Load History");
        loadHistoryButton.setOnAction(e -> loadSelectedHistory());

        historyTable = new TableView<>();
        historyItems = FXCollections.observableArrayList();
        historyTable.setItems(historyItems);
        configureHistoryTable();

        root.getChildren().addAll(
                new Label("Historical Weather Data"),
                buildDateControls(),
                historyTable
        );

        return root;
    }

    private HBox buildDateControls() {
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);

        controls.getChildren().addAll(
                new Label("Start:"), startDatePicker,
                new Label("End:"), endDatePicker,
                loadHistoryButton
        );

        return controls;
    }

    private VBox buildAnalysisPane() {
        VBox root = new VBox(10);

        dailyTrendChart = new WeatherTrendChart(600, 200);
        dailyTrendChart.setMetric(WeatherMetric.TEMPERATURE);

        weeklyTrendChart = new WeatherTrendChart(600, 200);
        weeklyTrendChart.setMetric(WeatherMetric.TEMPERATURE);

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

        TableColumn<WeatherData, String> tempCol = new TableColumn<>("Temp (°F)");
        tempCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getTemperature())));

        TableColumn<WeatherData, String> feelsLikeCol = new TableColumn<>("Feels Like (°F)");
        feelsLikeCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getFeelsLike())));

        TableColumn<WeatherData, String> humidityCol = new TableColumn<>("Humidity (%)");
        humidityCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getHumidity())));

        TableColumn<WeatherData, String> windCol = new TableColumn<>("Wind (mph)");
        windCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getWindSpeed())));

        TableColumn<WeatherData, String> solarCol = new TableColumn<>("Solar (W/m²)");
        solarCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getSolarIrradiance())));

        TableColumn<WeatherData, String> rainCol = new TableColumn<>("Rainfall (in)");
        rainCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getRainfall())));

        historyTable.getColumns().setAll(
                timestampCol, tempCol, feelsLikeCol, humidityCol, windCol, solarCol, rainCol
        );
    }

    private void configureForecastTable() {
        TableColumn<ForecastEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(dateFormatter)));

        TableColumn<ForecastEntry, String> tempCol = new TableColumn<>("Predicted Temp (°F)");
        tempCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFormattedTemperature()));

        TableColumn<ForecastEntry, String> confidenceCol = new TableColumn<>("Confidence");
        confidenceCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getConfidenceLabel()));

        forecastTable.getColumns().setAll(dateCol, tempCol, confidenceCol);
    }

    private void initializeDefaults() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(5));
        endDatePicker.setValue(today);
    }

    private void refreshAll() {
        refreshLiveWeather();
        loadSelectedHistory();
        loadTrendViews();
        loadForecast();
    }

    private void loadSelectedHistory() {
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
    }

    public void refreshLiveWeather() {
        if (dataProvider == null) {
            return;
        }

        setStatus(SystemStatus.LOADING, "Loading current weather...");

        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() {
                return dataProvider.getCurrentWeather();
            }
        };

        task.setOnSucceeded(e -> updateCurrentWeather(task.getValue()));
        task.setOnFailed(e -> {
            setStatus(SystemStatus.ERROR, "Failed to load current weather");
            showError(getErrorMessage(task));
        });

        runInBackground(task);
    }

    public void loadHistory(LocalDate start, LocalDate end) {
        if (dataProvider == null) {
            return;
        }

        Task<List<WeatherData>> task = new Task<>() {
            @Override
            protected List<WeatherData> call() {
                return dataProvider.getHistoricalWeather(start, end);
            }
        };

        task.setOnSucceeded(e -> historyItems.setAll(task.getValue()));
        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    public void loadForecast() {
        if (dataProvider == null) {
            return;
        }

        Task<List<ForecastEntry>> task = new Task<>() {
            @Override
            protected List<ForecastEntry> call() {
                return dataProvider.getForecast();
            }
        };

        task.setOnSucceeded(e -> forecastItems.setAll(task.getValue()));
        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    public void loadTrendViews() {
        if (dataProvider == null) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<WeatherData> daily = dataProvider.getDailyTrend();
                List<WeatherData> weekly = dataProvider.getWeeklyTrend();
                DailySummary summary = dataProvider.getDailySummary();

                Platform.runLater(() -> updateTrendViews(daily, weekly, summary));
                return null;
            }
        };

        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    private void updateTrendViews(List<WeatherData> daily,
                                  List<WeatherData> weekly,
                                  DailySummary summary) {
        dailyTrendChart.setData(daily);
        weeklyTrendChart.setData(weekly);

        if (summary == null) {
            dailyHighLowLabel.setText("Daily High/Low: --");
            return;
        }

        dailyHighLowLabel.setText(String.format(
                "Daily High/Low: %.2f °F / %.2f °F",
                summary.getHighTemp(),
                summary.getLowTemp()
        ));
    }

    public void updateCurrentWeather(WeatherData data) {
        if (data == null) {
            setStatus(SystemStatus.ERROR, "No weather data available");
            return;
        }

        temperatureCard.setValue(format(data.getTemperature()) + " °F");
        feelsLikeCard.setValue(format(data.getFeelsLike()) + " °F");
        humidityCard.setValue(format(data.getHumidity()) + " %");
        windCard.setValue(format(data.getWindSpeed()) + " mph");
        solarCard.setValue(format(data.getSolarIrradiance()) + " W/m²");
        rainfallCard.setValue(format(data.getRainfall()) + " in");

        timestampLabel.setText("Last updated: " + data.getFormattedTimestamp());

        SystemStatus status = data.getStatus() == null ? SystemStatus.LIVE : data.getStatus();
        setStatus(status, getStatusMessage(status));
    }

    private void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String getErrorMessage(Task<?> task) {
        return task.getException() == null
                ? "Unknown error"
                : task.getException().getMessage();
    }

    private String getStatusMessage(SystemStatus status) {
        switch (status) {
            case STALE:
                return "Data is stale";
            case CACHED:
                return "Showing cached data";
            case ERROR:
                return "Data error";
            case LOADING:
                return "Loading";
            case LIVE:
            default:
                return "Live data loaded";
        }
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Weather Dashboard Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}