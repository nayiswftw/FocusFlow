package com.focusflow;

public class Task {

    private final int id;
    private final String title;
    private final String priority;
    private final boolean completed;

    public Task(int id, String title, String priority, boolean completed) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }
}
