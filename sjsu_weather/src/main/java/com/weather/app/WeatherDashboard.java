package com.weather.app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
 * Main JavaFX UI for the weather application.
 *
 * Responsibilities:
 * - Display current weather values (cards)
 * - Display historical data (table)
 * - Display daily and weekly trend charts
 * - Display forecast results
 * - Handle user interactions (date selection, refresh)
 *
 * Important:
 * This class does NOT fetch or process raw data.
 * It depends entirely on DashboardDataProvider.
 */
public class WeatherDashboard extends Application {

    /**
     * Optional provider injected by Main before JavaFX launches.
     *
     * This lets Main and WeatherDashboard share the same configured app flow
     * instead of WeatherDashboard creating a separate provider on its own.
     */
    private static volatile DashboardDataProvider injectedDataProvider;

    /**
     * Called by Main to give the dashboard a preconfigured data provider.
     */
    public static void setInjectedDataProvider(DashboardDataProvider provider) {
        injectedDataProvider = provider;
    }

    /**
     * Main interface used by the UI to request current, historical, trend,
     * summary, and forecast data.
     */
    private DashboardDataProvider dataProvider;

    /**
     * Displays current system state such as Live, Cached, Stale, Loading, or Error.
     */
    private Label statusLabel;

    /**
     * Displays timestamp for the most recent weather reading.
     */
    private Label timestampLabel;

    /**
     * Displays high/low temperatures for the daily chart data.
     */
    private Label dailyHighLowLabel;

    /**
     * Dynamic title for the daily trend chart, including selected date.
     */
    private Label dailyTrendLabel;

    /**
     * Dynamic title for the weekly trend chart, including selected 7-day range.
     */
    private Label weeklyTrendLabel;

    /**
     * Current weather value cards.
     *
     * ValueCard is a reusable UI component that shows one label + one value.
     */
    private ValueCard temperatureCard;
    private ValueCard feelsLikeCard;
    private ValueCard humidityCard;
    private ValueCard windCard;
    private ValueCard solarCard;
    private ValueCard rainfallCard;

    /**
     * Buttons for user-triggered actions.
     */
    private Button refreshButton;
    private Button retryButton;
    private Button loadHistoryButton;
    private Button refreshForecastButton;

    /**
     * Date pickers for selecting the historical data range.
     */
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    /**
     * Table showing historical WeatherData rows.
     */
    private TableView<WeatherData> historyTable;

    /**
     * Observable list backing the history table.
     *
     * JavaFX automatically refreshes the table when this list changes.
     */
    private ObservableList<WeatherData> historyItems;

    /**
     * Table showing forecast rows.
     */
    private TableView<ForecastEntry> forecastTable;

    /**
     * Observable list backing the forecast table.
     */
    private ObservableList<ForecastEntry> forecastItems;

    /**
     * Custom chart for one selected day.
     */
    private WeatherTrendChart dailyTrendChart;

    /**
     * Custom chart for seven-day trend.
     */
    private WeatherTrendChart weeklyTrendChart;

    /**
     * Formatter used in the forecast table.
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Shorter formatter used in chart labels.
     */
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");

    /**
     * JavaFX application startup method.
     *
     * Builds the UI, connects a DashboardDataProvider, then loads initial data.
     */
    @Override
    public void start(Stage stage) {
        /*
         * Prefer the provider injected by Main.
         *
         * If no provider was injected, create the default composition here so
         * WeatherDashboard can still run independently.
         */
        DashboardDataProvider injected = injectedDataProvider;
        injectedDataProvider = null;

        if (injected != null) {
            dataProvider = injected;
        } else {
            WeatherAppComposition composition = WeatherAppComposition.createDefault();
            System.out.println("Dashboard using CSV file: " + composition.getCsvPath());
            dataProvider = composition.createDashboardDataProvider();
        }

        /*
         * BorderPane gives the dashboard a simple layout:
         * - top = status + current weather cards
         * - center = history table + analysis/forecast area
         */
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildTopSection());
        root.setCenter(buildCenterSection());

        // Set initial date picker values before requesting history data.
        initializeDefaults();

        // Create and display the JavaFX window.
        Scene scene = new Scene(root, 1500, 940);
        stage.setTitle("SJSU Weather Dashboard");
        stage.setScene(scene);
        stage.show();

        // Initial data load when the dashboard opens.
        refreshLiveWeather();
        loadHistory(startDatePicker.getValue(), endDatePicker.getValue());
        loadForecast();
        loadTrendViews();
    }

    /**
     * Builds the top portion of the dashboard.
     *
     * Includes:
     * - status bar
     * - last updated timestamp
     * - Refresh/Retry buttons
     * - current weather cards
     */
    private VBox buildTopSection() {
        VBox root = new VBox(12);

        // Horizontal status bar across the top.
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(12));

        statusLabel = new Label("Status: Waiting for data");
        statusLabel.setFont(Font.font(15));

        timestampLabel = new Label("Last updated: --");

        refreshButton = new Button("Refresh");
        retryButton = new Button("Retry");

        // Both buttons use the same reload workflow.
        refreshButton.setOnAction(e -> handleRetry());
        retryButton.setOnAction(e -> handleRetry());

        /*
         * Spacer consumes extra horizontal space so the buttons stay on the
         * right side of the status bar.
         */
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, timestampLabel, spacer, refreshButton, retryButton);

        // Grid for current weather cards.
        GridPane cards = new GridPane();
        cards.setHgap(10);
        cards.setVgap(10);

        // Cards are initialized with "--" until data loads.
        temperatureCard = new ValueCard("Temperature", "--");
        feelsLikeCard = new ValueCard("Feels Like", "--");
        humidityCard = new ValueCard("Humidity", "--");
        windCard = new ValueCard("Wind", "--");
        solarCard = new ValueCard("Solar", "--");
        rainfallCard = new ValueCard("Rainfall", "--");

        // Arrange cards in two rows.
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
     * Builds the center split pane.
     *
     * Left side: historical table.
     * Right side: charts and forecast.
     */
    private SplitPane buildCenterSection() {
        SplitPane split = new SplitPane();
        split.getItems().addAll(buildHistoryPane(), buildAnalysisPane());
        split.setDividerPositions(0.52);
        return split;
    }

    /**
     * Builds the historical data section.
     *
     * Includes date pickers, Load History button, and the history table.
     */
    private VBox buildHistoryPane() {
        VBox root = new VBox(10);

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();

        loadHistoryButton = new Button("Load History");
        loadHistoryButton.setOnAction(e -> loadSelectedHistory());

        /*
         * Table setup:
         * historyItems is the observable backing list.
         * configureHistoryTable defines columns and value mapping.
         */
        historyTable = new TableView<>();
        historyItems = FXCollections.observableArrayList();
        historyTable.setItems(historyItems);
        configureHistoryTable();

        // Date selector controls.
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                new Label("Start:"), startDatePicker,
                new Label("End:"), endDatePicker,
                loadHistoryButton
        );

        root.getChildren().addAll(
                new Label("Historical Weather Data"),
                controls,
                historyTable
        );

        return root;
    }

    /**
     * Builds the analysis section.
     *
     * Includes daily/weekly charts, high-low label, forecast refresh button,
     * and forecast table.
     */
    private VBox buildAnalysisPane() {
        VBox root = new VBox(10);

        dailyTrendLabel = new Label("Daily Temperature Trend");
        weeklyTrendLabel = new Label("Weekly Temperature Trend");

        /*
         * Charts are metric-based.
         * Current dashboard uses temperature, but WeatherTrendChart can support
         * other WeatherMetric values too.
         */
        dailyTrendChart = new WeatherTrendChart(600, 200);
        dailyTrendChart.setMetric(WeatherMetric.TEMPERATURE);

        weeklyTrendChart = new WeatherTrendChart(600, 200);
        weeklyTrendChart.setMetric(WeatherMetric.TEMPERATURE);

        dailyHighLowLabel = new Label("Daily High/Low: --");

        refreshForecastButton = new Button("Refresh Forecast");
        refreshForecastButton.setOnAction(e -> loadForecast());

        // Forecast table setup.
        forecastTable = new TableView<>();
        forecastItems = FXCollections.observableArrayList();
        forecastTable.setItems(forecastItems);
        configureForecastTable();

        root.getChildren().addAll(
                dailyTrendLabel,
                dailyTrendChart,
                weeklyTrendLabel,
                weeklyTrendChart,
                dailyHighLowLabel,
                refreshForecastButton,
                forecastTable
        );

        return root;
    }

    /**
     * Configures columns for the historical weather table.
     *
     * Each column extracts one value from WeatherData and formats it for display.
     */
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

    /**
     * Configures columns for the forecast table.
     *
     * Forecast rows come from ForecastEntry objects generated by the forecast subsystem.
     */
    private void configureForecastTable() {
        TableColumn<ForecastEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(dateFormatter)));

        TableColumn<ForecastEntry, String> tempCol = new TableColumn<>("Predicted Temp (°F)");
        tempCol.setCellValueFactory(cell ->
                new SimpleStringProperty(format(cell.getValue().getPredictedTemperature())));

        TableColumn<ForecastEntry, String> confidenceCol = new TableColumn<>("Confidence");
        confidenceCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getConfidenceLabel()));

        forecastTable.getColumns().setAll(dateCol, tempCol, confidenceCol);
    }

    /**
     * Sets initial date picker values.
     *
     * Default is the last 5 days ending today.
     */
    private void initializeDefaults() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(5));
        endDatePicker.setValue(today);
    }

    /**
     * Validates date picker values before loading history.
     */
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

    /**
     * Loads the most recent weather reading for the current weather cards.
     *
     * Uses a JavaFX Task so data retrieval does not block the UI thread.
     */
    public void refreshLiveWeather() {
        if (dataProvider == null) {
            return;
        }

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
            showError(getErrorMessage(task));
        });

        runInBackground(task);
    }

    /**
     * Loads historical data for the selected date range.
     *
     * Also updates chart data:
     * - Table shows full selected range.
     * - Daily chart shows selected end date only.
     * - Weekly chart shows 7-day window ending on selected end date.
     */
    public void loadHistory(LocalDate start, LocalDate end) {
        if (dataProvider == null) {
            return;
        }

        Task<List<WeatherData>> task = new Task<List<WeatherData>>() {
            @Override
            protected List<WeatherData> call() {
                return dataProvider.getHistoricalWeather(start, end);
            }
        };

        task.setOnSucceeded(e -> {
            // Full selected date range goes into the historical table.
            List<WeatherData> selectedRange = task.getValue();
            historyItems.setAll(selectedRange);

            /*
             * Trend chart logic:
             *
             * - Historical table:
             *   Shows the full selected date range from the date pickers.
             *
             * - Daily trend chart:
             *   Shows only the selected END date. This answers:
             *   "How did temperature change throughout that one day?"
             *
             * - Weekly trend chart:
             *   Shows the 7-day period ending on the selected END date.
             *   This answers:
             *   "How did temperature change across the most recent week ending on that day?"
             */
            LocalDate dailyDate = end;
            LocalDate weekStart = end.minusDays(6);

            // Select only records from the selected end date.
            List<WeatherData> dailyData = selectedRange.stream()
                    .filter(w -> w.getTimestamp() != null)
                    .filter(w -> w.getTimestamp().toLocalDate().equals(dailyDate))
                    .collect(Collectors.toList());

            // Select records from the 7-day window ending on the selected end date.
            List<WeatherData> weeklyData = selectedRange.stream()
                    .filter(w -> w.getTimestamp() != null)
                    .filter(w -> {
                        LocalDate date = w.getTimestamp().toLocalDate();
                        return !date.isBefore(weekStart) && !date.isAfter(end);
                    })
                    .collect(Collectors.toList());

            // Push filtered data into the custom chart components.
            dailyTrendChart.setData(dailyData);
            weeklyTrendChart.setData(weeklyData);

            // Update chart titles and high/low label to match displayed chart data.
            updateTrendLabels(dailyDate, weekStart, end);
            updateHighLowForDailyData(dailyData);
        });

        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    /**
     * Loads forecast entries into the forecast table.
     *
     * Forecast data comes from DashboardDataProvider, which delegates to the
     * forecast/cache subsystem.
     */
    public void loadForecast() {
        if (dataProvider == null) {
            return;
        }

        Task<List<ForecastEntry>> task = new Task<List<ForecastEntry>>() {
            @Override
            protected List<ForecastEntry> call() {
                return dataProvider.getForecast();
            }
        };

        task.setOnSucceeded(e -> forecastItems.setAll(task.getValue()));
        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    /**
     * Loads default trend views from the data provider.
     *
     * This is used during startup. The provider decides what "daily" and
     * "weekly" mean by default, usually based on the latest available dataset date.
     */
    public void loadTrendViews() {
        if (dataProvider == null) {
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                List<WeatherData> daily = dataProvider.getDailyTrend();
                List<WeatherData> weekly = dataProvider.getWeeklyTrend();
                DailySummary summary = dataProvider.getDailySummary();

                /*
                 * UI updates must run on the JavaFX Application Thread.
                 * Platform.runLater schedules the chart/label changes safely.
                 */
                Platform.runLater(() -> {
                    dailyTrendChart.setData(daily);
                    weeklyTrendChart.setData(weekly);

                    if (summary != null) {
                        dailyHighLowLabel.setText(String.format(
                                "Latest Data High/Low: %.2f °F / %.2f °F",
                                summary.getHighTemp(),
                                summary.getLowTemp()
                        ));
                    } else {
                        dailyHighLowLabel.setText("Latest Data High/Low: --");
                    }
                });

                return null;
            }
        };

        task.setOnFailed(e -> showError(getErrorMessage(task)));

        runInBackground(task);
    }

    /**
     * Updates chart labels to show the exact date/range being plotted.
     */
    private void updateTrendLabels(LocalDate dailyDate, LocalDate weekStart, LocalDate weekEnd) {
        dailyTrendLabel.setText(
                "Daily Temperature Trend (" + dailyDate.format(shortDateFormatter) + ")"
        );

        weeklyTrendLabel.setText(
                "Weekly Temperature Trend (" +
                        weekStart.format(shortDateFormatter) +
                        " - " +
                        weekEnd.format(shortDateFormatter) +
                        ")"
        );
    }

    /**
     * Updates the high/low summary for the daily chart.
     */
    private void updateHighLowForDailyData(List<WeatherData> dailyData) {
        if (dailyData == null || dailyData.isEmpty()) {
            dailyHighLowLabel.setText("Daily High/Low: --");
            return;
        }

        double high = dailyData.stream()
                .mapToDouble(WeatherData::getTemperature)
                .max()
                .orElse(0);

        double low = dailyData.stream()
                .mapToDouble(WeatherData::getTemperature)
                .min()
                .orElse(0);

        dailyHighLowLabel.setText(String.format(
                "Daily High/Low: %.2f °F / %.2f °F",
                high,
                low
        ));
    }

    /**
     * Updates current weather cards and status display.
     */
    public void updateCurrentWeather(WeatherData data) {
        if (data == null) {
            setStatus(SystemStatus.ERROR, "No weather data available");
            return;
        }

        // Update card values with units.
        temperatureCard.setValue(format(data.getTemperature()) + " °F");
        feelsLikeCard.setValue(format(data.getFeelsLike()) + " °F");
        humidityCard.setValue(format(data.getHumidity()) + " %");
        windCard.setValue(format(data.getWindSpeed()) + " mph");
        solarCard.setValue(format(data.getSolarIrradiance()) + " W/m²");
        rainfallCard.setValue(format(data.getRainfall()) + " in");

        timestampLabel.setText("Last updated: " + data.getFormattedTimestamp());

        // Null status is treated as live data by default.
        SystemStatus status = data.getStatus();
        if (status == null) {
            status = SystemStatus.LIVE;
        }

        // Convert SystemStatus enum into user-facing status message.
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

    /**
     * Handles Refresh/Retry actions.
     *
     * Reloads the current weather, selected history range, and forecast.
     */
    public void handleRetry() {
        refreshLiveWeather();
        loadHistory(startDatePicker.getValue(), endDatePicker.getValue());
        loadForecast();
    }

    /**
     * Runs a JavaFX Task on a background daemon thread.
     *
     * This keeps the UI responsive while data is loading.
     */
    private void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Extracts a safe error message from a failed Task.
     */
    private String getErrorMessage(Task<?> task) {
        return task.getException() == null
                ? "Unknown error"
                : task.getException().getMessage();
    }

    /**
     * Formats numeric values to two decimal places.
     */
    private String format(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Updates the status label text and color based on SystemStatus.
     */
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

    /**
     * Shows a JavaFX error dialog.
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Weather Dashboard Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}