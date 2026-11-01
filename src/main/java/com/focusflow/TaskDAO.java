package com.focusflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            throw databaseError("Could not add task.", e);
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
            throw databaseError("Could not load tasks.", e);
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
            throw databaseError("Could not complete task.", e);
        }
    }

    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw databaseError("Could not delete task.", e);
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
            throw databaseError("Could not count tasks.", e);
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
            throw databaseError("Could not save study session.", e);
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
            throw databaseError("Could not load today's study time.", e);
        }
    }

    public int getTotalStudyMinutes() {
        String sql = "SELECT COALESCE(SUM(duration), 0) FROM sessions";

        try (Connection con = Database.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw databaseError("Could not load total study time.", e);
        }
    }

    public Map<LocalDate, Integer> getStudyMinutesByDate(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT session_date, COALESCE(SUM(duration), 0)
            FROM sessions
            WHERE session_date BETWEEN ? AND ?
            GROUP BY session_date
            ORDER BY session_date
        """;
        Map<LocalDate, Integer> studyMinutes = new LinkedHashMap<>();

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    studyMinutes.put(
                        LocalDate.parse(rs.getString(1)),
                        rs.getInt(2)
                    );
                }
            }

            return studyMinutes;
        } catch (SQLException e) {
            throw databaseError("Could not load weekly study activity.", e);
        }
    }

    public int getCurrentStudyStreak() {
        LocalDate today = LocalDate.now();
        LocalDate firstDate = today.minusYears(100);
        String sql = """
            SELECT DISTINCT session_date
            FROM sessions
            WHERE session_date >= ?
            ORDER BY session_date DESC
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, firstDate.toString());

            try (ResultSet rs = ps.executeQuery()) {
                LocalDate expectedDate = today;
                List<LocalDate> dates = new ArrayList<>();

                while (rs.next()) {
                    dates.add(LocalDate.parse(rs.getString(1)));
                }

                if (dates.isEmpty() || !dates.get(0).equals(today)) {
                    expectedDate = today.minusDays(1);
                }

                int streak = 0;
                for (LocalDate date : dates) {
                    if (date.equals(expectedDate)) {
                        streak++;
                        expectedDate = expectedDate.minusDays(1);
                    } else if (date.isBefore(expectedDate)) {
                        break;
                    }
                }
                return streak;
            }
        } catch (SQLException e) {
            throw databaseError("Could not calculate the study streak.", e);
        }
    }

    private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }
}
