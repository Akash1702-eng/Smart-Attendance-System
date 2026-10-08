package dao;

import model.AttendanceSession;
import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceSessionDAO {
    
    public boolean createSession(AttendanceSession session) {
        String sql = "INSERT INTO attendance_sessions (session_id, course_code, start_time, end_time, is_active) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, session.getSessionId());
            pstmt.setString(2, session.getCourseCode());
            pstmt.setString(3, session.getStartTime().toString());
            pstmt.setString(4, session.getEndTime().toString());
            pstmt.setInt(5, session.isActive() ? 1 : 0);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating attendance session: " + e.getMessage());
            return false;
        }
    }
    
    public Optional<AttendanceSession> getSession(String sessionId) {
        String sql = "SELECT * FROM attendance_sessions WHERE session_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, sessionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AttendanceSession session = new AttendanceSession();
                    session.setSessionId(rs.getString("session_id"));
                    session.setCourseCode(rs.getString("course_code"));
                    session.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
                    session.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
                    session.setActive(rs.getInt("is_active") == 1);
                    return Optional.of(session);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance session: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public List<AttendanceSession> getActiveSessions() {
        List<AttendanceSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM attendance_sessions WHERE is_active = 1 ORDER BY start_time DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                AttendanceSession session = new AttendanceSession();
                session.setSessionId(rs.getString("session_id"));
                session.setCourseCode(rs.getString("course_code"));
                session.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
                session.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
                session.setActive(rs.getInt("is_active") == 1);
                sessions.add(session);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting active sessions: " + e.getMessage());
        }
        
        return sessions;
    }
    
    public boolean deactivateSession(String sessionId) {
        String sql = "UPDATE attendance_sessions SET is_active = 0 WHERE session_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, sessionId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating session: " + e.getMessage());
            return false;
        }
    }
}
