package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Attendance {
    private String studentId;
    private LocalDate date;
    private LocalTime time;
    private String courseCode;
    private String status;
    
    public Attendance() {}
    
    public Attendance(String studentId, LocalDate date, LocalTime time, String courseCode) {
        this.studentId = studentId;
        this.date = date;
        this.time = time;
        this.courseCode = courseCode;
        this.status = "PRESENT";
    }
    
    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return studentId + " - " + date + " - " + courseCode;
    }
}
