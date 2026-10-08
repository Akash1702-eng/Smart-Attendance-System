package dao;

import model.Attendance;
import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    // ============================================================
    //    REQUIRED BY AttendanceService — check if already marked
    // ============================================================
    public boolean isAttendanceMarked(String studentId, String courseCode, LocalDate date) {

        String sql = "SELECT 1 FROM attendance WHERE student_id = ? AND course_code = ? AND date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ps.setString(2, courseCode);
            ps.setString(3, date.toString());

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            System.err.println("isAttendanceMarked ERROR: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    //      REQUIRED BY AttendanceService — ADD ATTENDANCE
    // ============================================================
    public boolean addAttendance(Attendance a) {

        String sql = "INSERT INTO attendance (student_id, date, time, course_code, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, a.getStudentId());
            ps.setString(2, a.getDate().toString());
            ps.setString(3, a.getTime().toString());
            ps.setString(4, a.getCourseCode());
            ps.setString(5, "PRESENT"); // default if not provided

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {

            // UNIQUE constraint violation
            if (e.getMessage().contains("UNIQUE")) {
                System.out.println("Duplicate attendance ignored.");
                return true; // do NOT fail the flow
            }

            System.err.println("addAttendance ERROR: " + e.getMessage());
            return false;
        }
    }


    // ============================================================
    //    YOUR ORIGINAL FUNCTIONALITY — UNCHANGED
    // ============================================================
    public boolean markAttendance(Attendance attendance) {
        String checkSql = "SELECT 1 FROM attendance WHERE student_id = ? AND date = ? AND course_code = ?";
        String insertSql = "INSERT INTO attendance (student_id, date, time, course_code, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, attendance.getStudentId());
                checkStmt.setString(2, attendance.getDate().toString());
                checkStmt.setString(3, attendance.getCourseCode());

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Attendance already marked for student: " + attendance.getStudentId());
                        return false;
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, attendance.getStudentId());
                insertStmt.setString(2, attendance.getDate().toString());
                insertStmt.setString(3, attendance.getTime().toString());
                insertStmt.setString(4, attendance.getCourseCode());
                insertStmt.setString(5, attendance.getStatus());

                int rowsAffected = insertStmt.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Error marking attendance: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }


    public List<Attendance> getAttendanceByStudent(String studentId) {
        return getAttendanceByStudent(studentId, null);
    }

    public List<Attendance> getAttendanceByStudent(String studentId, String courseCode) {
        List<Attendance> attendanceList = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, s.name as student_name FROM attendance a " +
            "JOIN students s ON a.student_id = s.student_id " +
            "WHERE a.student_id = ?");

        if (courseCode != null && !courseCode.isEmpty()) {
            sql.append(" AND a.course_code = ?");
        }
        sql.append(" ORDER BY a.date DESC, a.time DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setString(1, studentId);
            if (courseCode != null && !courseCode.isEmpty()) {
                pstmt.setString(2, courseCode);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = extractAttendanceFromResultSet(rs);
                    attendanceList.add(attendance);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting attendance by student: " + e.getMessage());
        }

        return attendanceList;
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.*, s.name as student_name FROM attendance a " +
                    "JOIN students s ON a.student_id = s.student_id " +
                    "WHERE a.date = ? ORDER BY a.time";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = extractAttendanceFromResultSet(rs);
                    attendanceList.add(attendance);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting attendance by date: " + e.getMessage());
        }

        return attendanceList;
    }

    public String getAttendanceSummary(LocalDate date) {
        String sql = "SELECT course_code, COUNT(*) as count FROM attendance WHERE date = ? GROUP BY course_code";

        StringBuilder summary = new StringBuilder();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (summary.length() > 0) {
                        summary.append(", ");
                    }
                    summary.append(rs.getString("course_code"))
                          .append(": ")
                          .append(rs.getInt("count"))
                          .append(" students");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting attendance summary: " + e.getMessage());
        }

        return summary.toString();
    }

    public List<String> getAllCourses() {
        List<String> courses = new ArrayList<>();
        String sql = "SELECT course_code, course_name FROM courses ORDER BY course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String courseInfo = rs.getString("course_code") + " - " + rs.getString("course_name");
                courses.add(courseInfo);
            }

        } catch (SQLException e) {
            System.err.println("Error getting courses: " + e.getMessage());
        }

        return courses;
    }

    private Attendance extractAttendanceFromResultSet(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setStudentId(rs.getString("student_id"));
        attendance.setDate(LocalDate.parse(rs.getString("date")));
        attendance.setTime(LocalTime.parse(rs.getString("time")));
        attendance.setCourseCode(rs.getString("course_code"));
        attendance.setStatus(rs.getString("status"));
        return attendance;
    }
}
