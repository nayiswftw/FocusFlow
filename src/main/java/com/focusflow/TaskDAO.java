package com.focusflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    public void addTask(String title, String priority) {
        String sql = """
            INSERT INTO tasks(title, priority)
            VALUES(?, ?)
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, priority);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY completed, id DESC";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tasks.add(new Task(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("priority"),
                    rs.getInt("completed") == 1
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
    }

    public void completeTask(int id) {
        String sql = """
            UPDATE tasks
            SET completed = 1
            WHERE id = ?
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTotalTasks() {
        return getCount("SELECT COUNT(*) FROM tasks");
    }

    public int getCompletedTasks() {
        return getCount("SELECT COUNT(*) FROM tasks WHERE completed = 1");
    }

    private int getCount(String sql) {
        try (Connection con = Database.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void saveSession(int minutes) {
        String sql = """
            INSERT INTO sessions(duration, session_date)
            VALUES(?, ?)
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, minutes);
            ps.setString(2, LocalDate.now().toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTodayStudyMinutes() {
        String sql = """
            SELECT COALESCE(SUM(duration), 0)
            FROM sessions
            WHERE session_date = ?
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, LocalDate.now().toString());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getTotalStudyMinutes() {
        String sql = "SELECT COALESCE(SUM(duration), 0) FROM sessions";

        try (Connection con = Database.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
