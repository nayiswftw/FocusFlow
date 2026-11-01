package com.focusflow;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FocusFlowApp extends Application {

    private final TaskDAO dao = new TaskDAO();

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

    private static final int FOCUS_MINUTES = 25;
    private static final int BREAK_MINUTES = 5;
    private static final int DAILY_GOAL = 120;

    @Override
    public void start(Stage stage) {
        Database.initialize();

        BorderPane root = new BorderPane();
        root.setLeft(createSidebar());

        VBox dashboard = createDashboard();
        root.setCenter(dashboard);
        root.setStyle("-fx-background-color: #10121B;");

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("FocusFlow - Productivity Tracker");
        stage.setMinWidth(850);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();

        refreshDashboard();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(22);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #181B28;");

        Label logo = new Label("◈ FocusFlow");
        logo.setFont(Font.font("Arial", 24));
        logo.setTextFill(Color.web("#A78BFA"));

        Label subtitle = new Label("STUDY SMARTER");
        subtitle.setTextFill(Color.GRAY);

        Button dashboardBtn = navButton("▦  Dashboard");
        Button tasksBtn = navButton("✓  My Tasks");
        Button timerBtn = navButton("◷  Focus Timer");
        Button analyticsBtn = navButton("▤  Analytics");

        dashboardBtn.setOnAction(e -> showMessage("You are already on Dashboard."));
        tasksBtn.setOnAction(e -> {
            refreshTasks();
            showMessage("Your task list is displayed on the dashboard.");
        });
        timerBtn.setOnAction(e -> showMessage("Use the Focus Timer panel on the dashboard."));
        analyticsBtn.setOnAction(e -> showAnalytics());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("FocusFlow v1.0");
        footer.setTextFill(Color.GRAY);

        sidebar.getChildren().addAll(
            logo, subtitle,
            dashboardBtn, tasksBtn,
            timerBtn, analyticsBtn,
            spacer, footer
        );

        return sidebar;
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(13));
        button.setStyle("""
            -fx-background-color: #242839;
            -fx-text-fill: white;
            -fx-background-radius: 9;
            -fx-font-size: 14px;
            -fx-cursor: hand;
        """);
        return button;
    }

    private VBox createDashboard() {
        VBox content = new VBox(22);
        content.setPadding(new Insets(30));

        Label heading = new Label("Welcome to FocusFlow 👋");
        heading.setFont(Font.font("Arial", 27));
        heading.setTextFill(Color.WHITE);

        Label description = new Label("Stay focused. Track progress. Achieve more.");
        description.setTextFill(Color.LIGHTGRAY);

        HBox stats = new HBox(15);

        totalTasksLabel = new Label("0");
        completedLabel = new Label("0");
        studyLabel = new Label("0 min");

        stats.getChildren().addAll(
            statCard("Total Tasks", totalTasksLabel, "#818CF8"),
            statCard("Completed", completedLabel, "#34D399"),
            statCard("Study Today", studyLabel, "#FBBF24")
        );

        VBox timerCard = createTimerCard();

        HBox lowerSection = new HBox(20);

        VBox tasksCard = createTasksCard();
        VBox analyticsCard = createGoalCard();

        HBox.setHgrow(tasksCard, Priority.ALWAYS);
        HBox.setHgrow(analyticsCard, Priority.ALWAYS);

        lowerSection.getChildren().addAll(tasksCard, analyticsCard);

        content.getChildren().addAll(
            heading,
            description,
            stats,
            timerCard,
            lowerSection
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
            -fx-background: #10121B;
            -fx-background-color: #10121B;
            -fx-border-color: transparent;
        """);

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return wrapper;
    }

    private VBox statCard(String title, Label value, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(220);
        card.setStyle("""
            -fx-background-color: #1C2030;
            -fx-background-radius: 14;
        """);

        Label heading = new Label(title);
        heading.setTextFill(Color.LIGHTGRAY);

        value.setFont(Font.font("Arial", 26));
        value.setTextFill(Color.web(color));

        card.getChildren().addAll(heading, value);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createTimerCard() {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setStyle("""
            -fx-background-color: #24223B;
            -fx-background-radius: 16;
        """);

        modeLabel = new Label("FOCUS SESSION");
        modeLabel.setTextFill(Color.web("#A78BFA"));
        modeLabel.setFont(Font.font("Arial", 15));

        timerLabel = new Label("25:00");
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setFont(Font.font("Arial", 55));

        Label info = new Label("Stay concentrated and complete your work.");
        info.setTextFill(Color.LIGHTGRAY);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        startButton = new Button("Start Focus");
        Button resetButton = new Button("Reset");
        Button switchButton = new Button("Switch Mode");

        styleActionButton(startButton);
        styleActionButton(resetButton);
        styleActionButton(switchButton);

        startButton.setOnAction(e -> toggleTimer());
        resetButton.setOnAction(e -> resetTimer());
        switchButton.setOnAction(e -> switchMode());

        controls.getChildren().addAll(startButton, resetButton, switchButton);
        card.getChildren().addAll(modeLabel, timerLabel, info, controls);

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
            dao.saveSession(FOCUS_MINUTES);
            new Alert(Alert.AlertType.INFORMATION,
                "Great job! You completed a 25-minute focus session.")
                .show();
            refreshDashboard();
        } else {
            new Alert(Alert.AlertType.INFORMATION,
                "Break completed! Ready to focus again?")
                .show();
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

    private VBox createTasksCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: #1C2030;
            -fx-background-radius: 14;
        """);

        Label title = new Label("My Tasks");
        title.setFont(Font.font("Arial", 19));
        title.setTextFill(Color.WHITE);

        Button addButton = new Button("+ Add Task");
        styleActionButton(addButton);
        addButton.setOnAction(e -> addTaskDialog());

        HBox heading = new HBox();
        heading.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        heading.getChildren().addAll(title, spacer, addButton);

        taskContainer = new VBox(10);

        ScrollPane taskScroll = new ScrollPane(taskContainer);
        taskScroll.setFitToWidth(true);
        taskScroll.setPrefHeight(220);
        taskScroll.setStyle("""
            -fx-background: #1C2030;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
        """);

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

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            String title = titleField.getText().trim();

            if (result == ButtonType.OK && !title.isEmpty()) {
                dao.addTask(title, priorityBox.getValue());
                refreshDashboard();
            }
        });
    }

    private void refreshTasks() {
        if (taskContainer == null) return;

        taskContainer.getChildren().clear();

        for (Task task : dao.getAllTasks()) {
            Label taskName = new Label(task.getTitle());
            taskName.setTextFill(task.isCompleted() ? Color.GRAY : Color.WHITE);

            if (task.isCompleted()) {
                taskName.setStyle("-fx-strikethrough: true;");
            }

            Label priority = new Label(task.getPriority());
            priority.setTextFill(switch (task.getPriority()) {
                case "High" -> Color.SALMON;
                case "Medium" -> Color.GOLD;
                default -> Color.LIGHTGREEN;
            });

            Button complete = new Button(task.isCompleted() ? "Done" : "✓");
            Button delete = new Button("×");

            complete.setDisable(task.isCompleted());
            styleSmallButton(complete);
            styleSmallButton(delete);

            complete.setOnAction(e -> {
                dao.completeTask(task.getId());
                refreshDashboard();
            });

            delete.setOnAction(e -> {
                dao.deleteTask(task.getId());
                refreshDashboard();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, taskName, spacer, priority, complete, delete);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("""
                -fx-background-color: #292E40;
                -fx-background-radius: 8;
            """);

            taskContainer.getChildren().add(row);
        }

        if (taskContainer.getChildren().isEmpty()) {
            Label empty = new Label("No tasks yet. Add your first task!");
            empty.setTextFill(Color.LIGHTGRAY);
            taskContainer.getChildren().add(empty);
        }
    }

    private VBox createGoalCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: #1C2030;
            -fx-background-radius: 14;
        """);

        Label heading = new Label("Daily Study Goal");
        heading.setFont(Font.font("Arial", 19));
        heading.setTextFill(Color.WHITE);

        progressLabel = new Label("0 / 120 minutes");
        progressLabel.setTextFill(Color.LIGHTGRAY);

        goalProgress = new ProgressBar(0);
        goalProgress.setMaxWidth(Double.MAX_VALUE);
        goalProgress.setPrefHeight(15);

        Label tip = new Label("Consistency is the key to success!");
        tip.setTextFill(Color.web("#A78BFA"));
        tip.setWrapText(true);

        card.getChildren().addAll(heading, progressLabel, goalProgress, tip);
        return card;
    }

    private void showAnalytics() {
        int total = dao.getTotalTasks();
        int completed = dao.getCompletedTasks();
        int today = dao.getTodayStudyMinutes();
        int allTime = dao.getTotalStudyMinutes();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("FocusFlow Analytics");
        alert.setHeaderText("Your Productivity Report");

        alert.setContentText(
            "Total Tasks: " + total +
            "\nCompleted Tasks: " + completed +
            "\nPending Tasks: " + (total - completed) +
            "\nToday's Study: " + today + " minutes" +
            "\nAll-Time Study: " + allTime + " minutes" +
            "\nDaily Goal: " + DAILY_GOAL + " minutes"
        );

        alert.showAndWait();
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
        button.setStyle("""
            -fx-background-color: #7C3AED;
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-padding: 10 17;
            -fx-cursor: hand;
            -fx-font-weight: bold;
        """);
    }

    private void styleSmallButton(Button button) {
        button.setStyle("""
            -fx-background-color: #383E54;
            -fx-text-fill: white;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);
    }

    private void showMessage(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
