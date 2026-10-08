package model;

import java.time.LocalDateTime;

public class AttendanceSession {
    private String sessionId;
    private String courseCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isActive;
    
    public AttendanceSession() {}
    
    public AttendanceSession(String sessionId, String courseCode, LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.courseCode = courseCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isValid() {
        return isActive && LocalDateTime.now().isAfter(startTime) && LocalDateTime.now().isBefore(endTime);
    }
    
    @Override
    public String toString() {
        return sessionId + " - " + courseCode + " (Until: " + endTime.toLocalTime() + ")";
    }
}
