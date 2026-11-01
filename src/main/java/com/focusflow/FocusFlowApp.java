package com.focusflow;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class FocusFlowApp extends Application {

    private final TaskDAO dao = new TaskDAO();

    private BorderPane root;
    private Button activeNavButton;
    private Label totalTasksLabel;
    private Label completedLabel;
    private Label studyLabel;
    private Label timerLabel;
    private Label modeLabel;
    private Label progressLabel;
    private ProgressBar goalProgress;

    private VBox taskContainer;

    private Timeline timeline;
    private int remainingSeconds = 25 * 60;
    private boolean running = false;
    private boolean focusMode = true;

    private Button startButton;
    private TrayIcon trayIcon;

    private static final int FOCUS_MINUTES = 25;
    private static final int BREAK_MINUTES = 5;
    private static final int DAILY_GOAL = 120;

    @Override
    public void start(Stage stage) {
        Database.initialize();

        root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setLeft(createSidebar());
        root.setCenter(createDashboard());

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("FocusFlow - Productivity Tracker");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        initializeDesktopNotifications();
        refreshDashboard();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(22);
        sidebar.setPadding(new Insets(26, 16, 20, 16));
        sidebar.setPrefWidth(228);
        sidebar.setMinWidth(228);
        sidebar.getStyleClass().add("sidebar");

        Label brandMark = new Label("F");
        brandMark.getStyleClass().add("brand-mark");
        Label logo = new Label("FocusFlow");
        logo.getStyleClass().add("brand-name");
        HBox brand = new HBox(10, brandMark, logo);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("PERSONAL WORKSPACE");
        subtitle.getStyleClass().add("sidebar-caption");

        Label workspaceLabel = new Label("WORKSPACE");
        workspaceLabel.getStyleClass().add("nav-section-label");
        Button dashboardBtn = navButton("Overview");
        Button tasksBtn = navButton("My tasks");
        Button timerBtn = navButton("Focus timer");
        Button analyticsBtn = navButton("Analytics");
        setActiveNavigation(dashboardBtn);

        dashboardBtn.setOnAction(e -> navigateTo(dashboardBtn, this::createDashboard));
        tasksBtn.setOnAction(e -> navigateTo(tasksBtn, this::createTasksPage));
        timerBtn.setOnAction(e -> navigateTo(timerBtn, this::createTimerPage));
        analyticsBtn.setOnAction(e -> navigateTo(analyticsBtn, this::createAnalyticsPage));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label avatar = new Label("FF");
        avatar.getStyleClass().add("avatar");
        Label footer = new Label("Local workspace");
        footer.getStyleClass().add("sidebar-footer-text");
        HBox account = new HBox(10, avatar, footer);
        account.setAlignment(Pos.CENTER_LEFT);
        account.getStyleClass().add("sidebar-account");

        sidebar.getChildren().addAll(
            brand, subtitle,
            workspaceLabel,
            dashboardBtn, tasksBtn,
            timerBtn, analyticsBtn,
            spacer, account
        );

        return sidebar;
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        return button;
    }

    private void navigateTo(Button navigationButton, Supplier<VBox> pageFactory) {
        try {
            setActiveNavigation(navigationButton);
            root.setCenter(pageFactory.get());
            if ("Overview".equals(navigationButton.getText())) {
                refreshDashboard();
            }
        } catch (IllegalStateException e) {
            showDatabaseError(e);
        }
    }

    private void setActiveNavigation(Button navigationButton) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        activeNavButton = navigationButton;
        if (!navigationButton.getStyleClass().contains("nav-button-active")) {
            navigationButton.getStyleClass().add("nav-button-active");
        }
    }

    private VBox createDashboard() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(34, 36, 36, 36));
        content.getStyleClass().add("dashboard-content");

        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("page-title");

        Label description = new Label("A clear view of your work and focus for today.");
        description.getStyleClass().add("page-description");
        VBox pageHeader = new VBox(6, heading, description);
        pageHeader.getStyleClass().add("page-header");

        HBox stats = new HBox(14);
        stats.getStyleClass().add("stats-row");

        totalTasksLabel = new Label("0");
        completedLabel = new Label("0");
        studyLabel = new Label("0 min");

        stats.getChildren().addAll(
            statCard("Total Tasks", totalTasksLabel),
            statCard("Completed", completedLabel),
            statCard("Study Today", studyLabel)
        );

        VBox timerCard = createTimerCard();

        HBox lowerSection = new HBox(20);

        VBox tasksCard = createTasksCard(220);
        VBox analyticsCard = createGoalCard();

        HBox.setHgrow(tasksCard, Priority.ALWAYS);
        HBox.setHgrow(analyticsCard, Priority.ALWAYS);

        lowerSection.getChildren().addAll(tasksCard, analyticsCard);

        content.getChildren().addAll(
            pageHeader,
            stats,
            timerCard,
            lowerSection
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dashboard-scroll");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return wrapper;
    }

    private VBox createTasksPage() {
        VBox page = createPageFrame(
            "My tasks",
            "Plan your next study session and keep your priorities clear."
        );
        VBox tasksCard = createTasksCard(460);
        VBox.setVgrow(tasksCard, Priority.ALWAYS);
        page.getChildren().add(tasksCard);
        return wrapPage(page);
    }

    private VBox createTimerPage() {
        VBox page = createPageFrame(
            "Focus timer",
            "Work in focused intervals, then take a restorative break."
        );

        VBox timerCard = createTimerCard();
        timerCard.getStyleClass().add("timer-card-large");

        VBox guide = new VBox(8);
        guide.setPadding(new Insets(20));
        guide.getStyleClass().add("content-card");
        Label guideTitle = new Label("Your focus cycle");
        guideTitle.getStyleClass().add("card-title");
        Label guideText = new Label(
            "Focus for 25 minutes, then switch to a 5-minute break. " +
            "Completed focus sessions are saved to your study history."
        );
        guideText.getStyleClass().add("page-description");
        guideText.setWrapText(true);
        guide.getChildren().addAll(guideTitle, guideText);

        page.getChildren().addAll(timerCard, guide);
        return wrapPage(page);
    }

    private VBox createAnalyticsPage() {
        VBox page = createPageFrame(
            "Analytics",
            "Review your study habits and progress over time."
        );

        int total = dao.getTotalTasks();
        int completed = dao.getCompletedTasks();
        int today = dao.getTodayStudyMinutes();
        int allTime = dao.getTotalStudyMinutes();
        int streak = dao.getCurrentStudyStreak();
        int completionPercent = total == 0 ? 0 : completed * 100 / total;

        HBox stats = new HBox(14);
        stats.getStyleClass().add("stats-row");
        stats.getChildren().addAll(
            statCard("Tasks completed", new Label(completed + " / " + total)),
            statCard("Completion rate", new Label(completionPercent + "%")),
            statCard("Study streak", new Label(streak + (streak == 1 ? " day" : " days")))
        );

        HBox studySummary = new HBox(14);
        studySummary.getStyleClass().add("stats-row");
        studySummary.getChildren().addAll(
            statCard("Study today", new Label(formatDuration(today))),
            statCard("All-time study", new Label(formatDuration(allTime)))
        );

        LocalDate todayDate = LocalDate.now();
        LocalDate startDate = todayDate.minusDays(6);
        Map<LocalDate, Integer> minutesByDay = dao.getStudyMinutesByDate(startDate, todayDate);
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Minutes");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(22);
        chart.setBarGap(4);
        chart.setPrefHeight(310);
        chart.setMinHeight(260);
        chart.getStyleClass().add("weekly-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (LocalDate day = startDate; !day.isAfter(todayDate); day = day.plusDays(1)) {
            String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            series.getData().add(new XYChart.Data<>(label, minutesByDay.getOrDefault(day, 0)));
        }
        chart.getData().add(series);

        VBox chartCard = new VBox(14);
        chartCard.setPadding(new Insets(20));
        chartCard.getStyleClass().add("content-card");
        Label chartTitle = new Label("Study time · last 7 days");
        chartTitle.getStyleClass().add("card-title");
        chartCard.getChildren().addAll(chartTitle, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        page.getChildren().addAll(stats, studySummary, chartCard);
        return wrapPage(page);
    }

    private VBox wrapPage(VBox page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dashboard-scroll");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private VBox createPageFrame(String title, String description) {
        VBox page = new VBox(20);
        page.setPadding(new Insets(34, 36, 36, 36));
        page.getStyleClass().add("dashboard-content");

        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label subtitle = new Label(description);
        subtitle.getStyleClass().add("page-description");
        VBox header = new VBox(6, heading, subtitle);
        header.getStyleClass().add("page-header");
        page.getChildren().add(header);
        return page;
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (hours == 0) {
            return remainingMinutes + " min";
        }
        return remainingMinutes == 0
            ? hours + " hr"
            : hours + " hr " + remainingMinutes + " min";
    }

    private VBox statCard(String title, Label value) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.getStyleClass().add("stat-card");

        Label heading = new Label(title);
        heading.getStyleClass().add("stat-title");

        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(heading, value);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createTimerCard() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.getStyleClass().add("timer-card");

        modeLabel = new Label(focusMode ? "FOCUS SESSION" : "BREAK TIME");
        modeLabel.getStyleClass().add("timer-mode");

        timerLabel = new Label();
        updateTimerDisplay();
        timerLabel.getStyleClass().add("timer-value");

        Label info = new Label("Stay concentrated and complete your work.");
        info.getStyleClass().add("timer-description");
        VBox timerInfo = new VBox(8, modeLabel, info);
        timerInfo.setAlignment(Pos.CENTER_LEFT);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        startButton = new Button(
            running ? "Pause" : focusMode ? "Start Focus" : "Start Break"
        );
        Button resetButton = new Button("Reset");
        Button switchButton = new Button("Switch Mode");

        styleActionButton(startButton);
        resetButton.getStyleClass().add("button-secondary");
        switchButton.getStyleClass().add("button-secondary");

        startButton.setOnAction(e -> toggleTimer());
        resetButton.setOnAction(e -> resetTimer());
        switchButton.setOnAction(e -> switchMode());

        controls.getChildren().addAll(startButton, resetButton, switchButton);
        HBox timerLayout = new HBox(18, timerInfo, timerLabel, controls);
        timerLayout.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(timerInfo, Priority.ALWAYS);
        HBox.setHgrow(timerLabel, Priority.ALWAYS);
        timerLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(timerLayout);

        return card;
    }

    private void toggleTimer() {
        if (running) {
            timeline.stop();
            running = false;
            startButton.setText("Resume");
            return;
        }

        if (timeline == null) {
            timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;
                    updateTimerDisplay();

                    if (remainingSeconds <= 0) {
                        finishSession();
                    }
                })
            );
            timeline.setCycleCount(Timeline.INDEFINITE);
        }

        timeline.play();
        running = true;
        startButton.setText("Pause");
    }

    private void finishSession() {
        timeline.stop();
        running = false;

        if (focusMode) {
            try {
                dao.saveSession(FOCUS_MINUTES);
                refreshDashboard();
                showDesktopNotification(
                    "Focus session complete",
                    "Great work. Your 25-minute focus session has been recorded."
                );
            } catch (IllegalStateException e) {
                showDatabaseError(e);
            }
        } else {
            showDesktopNotification("Break complete", "Ready to focus again?");
        }

        switchMode();
    }

    private void resetTimer() {
        if (timeline != null) {
            timeline.stop();
        }

        running = false;
        remainingSeconds = focusMode ? FOCUS_MINUTES * 60 : BREAK_MINUTES * 60;
        updateTimerDisplay();
        startButton.setText(focusMode ? "Start Focus" : "Start Break");
    }

    private void switchMode() {
        if (timeline != null) {
            timeline.stop();
        }

        running = false;
        focusMode = !focusMode;

        remainingSeconds = focusMode ? FOCUS_MINUTES * 60 : BREAK_MINUTES * 60;
        modeLabel.setText(focusMode ? "FOCUS SESSION" : "BREAK TIME");
        startButton.setText(focusMode ? "Start Focus" : "Start Break");
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private VBox createTasksCard(int scrollHeight) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("content-card");

        Label title = new Label("My Tasks");
        title.getStyleClass().add("card-title");

        Button addButton = new Button("+ Add Task");
        styleActionButton(addButton);
        addButton.getStyleClass().add("button-compact");
        addButton.setOnAction(e -> addTaskDialog());

        HBox heading = new HBox();
        heading.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        heading.getChildren().addAll(title, spacer, addButton);

        taskContainer = new VBox(10);

        ScrollPane taskScroll = new ScrollPane(taskContainer);
        taskScroll.setFitToWidth(true);
        taskScroll.setPrefHeight(scrollHeight);
        taskScroll.getStyleClass().add("task-scroll");
        VBox.setVgrow(taskScroll, Priority.ALWAYS);

        card.getChildren().addAll(heading, taskScroll);
        refreshTasks();

        return card;
    }

    private void addTaskDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");

        TextField titleField = new TextField();
        titleField.setPromptText("Enter task name");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("High", "Medium", "Low");
        priorityBox.setValue("Medium");

        VBox form = new VBox(12,
            new Label("Task Name"),
            titleField,
            new Label("Priority"),
            priorityBox
        );

        form.setPadding(new Insets(15));
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm()
        );
        titleField.getStyleClass().add("text-input");
        priorityBox.getStyleClass().add("text-input");

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            String title = titleField.getText().trim();

            if (result != ButtonType.OK) {
                return;
            }
            if (title.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Add task");
                alert.setHeaderText("A task name is required");
                alert.setContentText("Enter a name before saving the task.");
                alert.show();
                return;
            }

            runDatabaseAction(() -> {
                dao.addTask(title, priorityBox.getValue());
                refreshDashboard();
            });
        });
    }

    private void refreshTasks() {
        if (taskContainer == null) return;

        taskContainer.getChildren().clear();

        for (Task task : dao.getAllTasks()) {
            Label taskName = new Label(task.getTitle());
            taskName.getStyleClass().add("task-name");

            if (task.isCompleted()) {
                taskName.getStyleClass().add("task-name-completed");
            }

            Label priority = new Label(task.getPriority());
            priority.getStyleClass().add("priority-badge");
            priority.getStyleClass().add(switch (task.getPriority()) {
                case "High" -> "priority-high";
                case "Medium" -> "priority-medium";
                default -> "priority-low";
            });

            Button complete = new Button(task.isCompleted() ? "Done" : "✓");
            Button delete = new Button("×");

            complete.setDisable(task.isCompleted());
            styleSmallButton(complete);
            styleSmallButton(delete);

            complete.setOnAction(e -> {
                runDatabaseAction(() -> {
                    dao.completeTask(task.getId());
                    refreshDashboard();
                });
            });

            delete.setOnAction(e -> {
                runDatabaseAction(() -> {
                    dao.deleteTask(task.getId());
                    refreshDashboard();
                });
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(10, taskName, spacer, priority, complete, delete);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.getStyleClass().add("task-row");
            HBox.setHgrow(taskName, Priority.ALWAYS);
            taskName.setMaxWidth(Double.MAX_VALUE);

            taskContainer.getChildren().add(row);
        }

        if (taskContainer.getChildren().isEmpty()) {
            Label empty = new Label("No tasks yet. Add your first task!");
            empty.getStyleClass().add("empty-state");
            taskContainer.getChildren().add(empty);
        }
    }

    private VBox createGoalCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("content-card");

        Label heading = new Label("Daily Study Goal");
        heading.getStyleClass().add("card-title");

        progressLabel = new Label("0 / 120 minutes");
        progressLabel.getStyleClass().add("progress-label");

        goalProgress = new ProgressBar(0);
        goalProgress.setMaxWidth(Double.MAX_VALUE);
        goalProgress.setPrefHeight(15);
        goalProgress.getStyleClass().add("goal-progress");

        Label tip = new Label("Consistency is the key to success!");
        tip.getStyleClass().add("goal-tip");
        tip.setWrapText(true);

        card.getChildren().addAll(heading, progressLabel, goalProgress, tip);
        return card;
    }

    private void refreshDashboard() {
        if (totalTasksLabel == null) return;

        int total = dao.getTotalTasks();
        int completed = dao.getCompletedTasks();
        int today = dao.getTodayStudyMinutes();

        totalTasksLabel.setText(String.valueOf(total));
        completedLabel.setText(String.valueOf(completed));
        studyLabel.setText(today + " min");

        refreshTasks();
        updateGoalProgress();
    }

    private void updateGoalProgress() {
        if (progressLabel == null || goalProgress == null) {
            return;
        }

        int today = dao.getTodayStudyMinutes();
        progressLabel.setText(today + " / " + DAILY_GOAL + " minutes");

        double percentage = Math.min(1.0, (double) today / DAILY_GOAL);
        goalProgress.setProgress(percentage);
    }

    private void styleActionButton(Button button) {
        button.getStyleClass().add("button-primary");
    }

    private void styleSmallButton(Button button) {
        button.getStyleClass().add("button-icon");
    }

    private void runDatabaseAction(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            showDatabaseError(e);
        }
    }

    private void showDatabaseError(IllegalStateException error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("FocusFlow");
        alert.setHeaderText("Your data could not be updated");
        alert.setContentText(error.getMessage());
        alert.show();
    }

    private void initializeDesktopNotifications() {
        try {
            if (!SystemTray.isSupported()) {
                return;
            }

            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
                graphics.setColor(new Color(9, 9, 11));
                graphics.drawString("F", 5, 12);
            } finally {
                graphics.dispose();
            }

            TrayIcon icon = new TrayIcon(image, "FocusFlow");
            icon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
        } catch (AWTException | HeadlessException | SecurityException e) {
            System.err.println("Desktop notifications are unavailable: " + e.getMessage());
        }
    }

    private void showDesktopNotification(String title, String message) {
        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                return;
            } catch (HeadlessException | SecurityException e) {
                System.err.println("Could not display a desktop notification: " + e.getMessage());
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (HeadlessException | SecurityException e) {
                System.err.println("Could not remove the FocusFlow notification icon: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
