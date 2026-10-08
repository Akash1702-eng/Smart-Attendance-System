package service;

import dao.AttendanceDAO;
import dao.AttendanceSessionDAO;
import dao.StudentDAO;
import model.Attendance;
import model.AttendanceSession;
import model.Student;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class AttendanceService {
    private StudentDAO studentDAO;
    private AttendanceDAO attendanceDAO;
    private AttendanceSessionDAO sessionDAO;
    
    public AttendanceService() {
        this.studentDAO = new StudentDAO();
        this.attendanceDAO = new AttendanceDAO();
        this.sessionDAO = new AttendanceSessionDAO();
    }
    
    public Optional<Student> registerStudent(Student student) {
        if (studentDAO.addStudent(student)) {
            return Optional.of(student);
        }
        return Optional.empty();
    }
    
    public Optional<Student> getStudent(String studentId) {
        return studentDAO.getStudentById(studentId);
    }
    
    public BufferedImage generateStudentQRCode(String studentId, int width, int height) {
        try {
            String qrData = "ATTENDANCE:" + studentId + ":" + System.currentTimeMillis();
            return QRCodeService.generateQRCodeImage(qrData, width, height);
        } catch (Exception e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }
    
    public boolean markAttendance(String studentId, String courseCode) {
        return markAttendance(studentId, courseCode, LocalDate.now(), LocalTime.now());
    }
    
    public boolean markAttendance(String studentId, String courseCode, LocalDate date, LocalTime time) {
        Optional<Student> studentOpt = studentDAO.getStudentById(studentId);
        if (!studentOpt.isPresent()) {
            return false;
        }
        
        Attendance attendance = new Attendance(studentId, date, time, courseCode);
        return attendanceDAO.markAttendance(attendance);
    }
    
    // NEW METHOD: Handle QR-based attendance from mobile app
    public boolean markAttendanceFromQR(String studentId, String qrData) {
        try {
            // Parse QR data (format: "ATTENDANCE_SESSION:SESSION_ID")
            String[] parts = qrData.split(":");
            if (parts.length >= 2 && "ATTENDANCE_SESSION".equals(parts[0])) {
                String sessionId = parts[1];
                
                // Get session information
                Optional<AttendanceSession> sessionOpt = sessionDAO.getSession(sessionId);
                if (!sessionOpt.isPresent()) {
                    System.err.println("Session not found: " + sessionId);
                    return false;
                }
                
                AttendanceSession session = sessionOpt.get();
                
                // Check if session is still valid
                if (!session.isActive()) {
                    System.err.println("Session is no longer active: " + sessionId);
                    return false;
                }
                
                if (session.getEndTime().isBefore(LocalDateTime.now())) {
                    System.err.println("Session has expired: " + sessionId);
                    return false;
                }
                
                // Verify student exists
                Optional<Student> studentOpt = studentDAO.getStudentById(studentId);
                if (!studentOpt.isPresent()) {
                    System.err.println("Student not found: " + studentId);
                    return false;
                }
                
                // Mark attendance for the course from the session
                boolean success = markAttendance(studentId, session.getCourseCode());
                if (success) {
                    System.out.println("Attendance marked successfully for student: " + studentId + 
                                     " in course: " + session.getCourseCode());
                } else {
                    System.err.println("Failed to mark attendance for student: " + studentId);
                }
                return success;
                
            } else {
                System.err.println("Invalid QR code format: " + qrData);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error processing QR attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public String processQRCode(String qrData, String courseCode) {
        try {
            String[] parts = qrData.split(":");
            if (parts.length >= 2 && "ATTENDANCE".equals(parts[0])) {
                String studentId = parts[1];
                Optional<Student> studentOpt = studentDAO.getStudentById(studentId);
                
                if (studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    if (markAttendance(studentId, courseCode)) {
                        return "SUCCESS: Attendance marked for " + student.getName() + " (" + studentId + ")";
                    } else {
                        return "ERROR: Attendance already marked for " + student.getName();
                    }
                } else {
                    return "ERROR: Student not found with ID: " + studentId;
                }
            }
            return "ERROR: Invalid QR code format";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    // Session management methods
    public String createAttendanceSession(String courseCode, int durationMinutes) {
        String sessionId = "SESSION_" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        
        AttendanceSession session = new AttendanceSession(sessionId, courseCode, startTime, endTime);
        if (sessionDAO.createSession(session)) {
            return sessionId;
        }
        return null;
    }
    
    public BufferedImage generateSessionQRCode(String sessionId, int width, int height) {
        try {
            String qrData = "ATTENDANCE_SESSION:" + sessionId;
            return QRCodeService.generateQRCodeImage(qrData, width, height);
        } catch (Exception e) {
            System.err.println("Error generating session QR code: " + e.getMessage());
            return null;
        }
    }
    
    public String processStudentAttendance(String qrData, String studentId) {
        try {
            String[] parts = qrData.split(":");
            if (parts.length >= 2 && "ATTENDANCE_SESSION".equals(parts[0])) {
                String sessionId = parts[1];
                Optional<AttendanceSession> sessionOpt = sessionDAO.getSession(sessionId);
                
                if (sessionOpt.isPresent()) {
                    AttendanceSession session = sessionOpt.get();
                    
                    if (!session.isActive()) {
                        return "ERROR: This attendance session is no longer active";
                    }
                    
                    if (session.getEndTime().isBefore(LocalDateTime.now())) {
                        return "ERROR: This attendance session has expired";
                    }
                    
                    if (markAttendance(studentId, session.getCourseCode())) {
                        return "SUCCESS: Attendance marked for " + session.getCourseCode();
                    } else {
                        return "ERROR: Attendance already marked for this session";
                    }
                } else {
                    return "ERROR: Invalid attendance session";
                }
            }
            return "ERROR: Invalid QR code format";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    // Get session by ID
    public Optional<AttendanceSession> getSession(String sessionId) {
        return sessionDAO.getSession(sessionId);
    }
    
    public List<AttendanceSession> getActiveSessions() {
        return sessionDAO.getActiveSessions();
    }
    
    public boolean deactivateSession(String sessionId) {
        return sessionDAO.deactivateSession(sessionId);
    }
    
    public List<Attendance> getStudentAttendance(String studentId) {
        return attendanceDAO.getAttendanceByStudent(studentId);
    }
    
    public String getTodayAttendanceSummary() {
        return attendanceDAO.getAttendanceSummary(LocalDate.now());
    }
    
    public boolean studentExists(String studentId) {
        return studentDAO.studentExists(studentId);
    }
    
    public List<Student> getAllStudents() {
        return studentDAO.getAllStudents();
    }
    
    public List<String> getAllCourses() {
        return attendanceDAO.getAllCourses();
    }
    
    public List<Attendance> getTodayAttendance() {
        return attendanceDAO.getAttendanceByDate(LocalDate.now());
    }
    
    // Delete methods
    public boolean deleteStudent(String studentId) {
        return studentDAO.deleteStudent(studentId);
    }
    
    public boolean deleteStudentWithAttendance(String studentId) {
        return studentDAO.deleteStudentWithAttendance(studentId);
    }
    
    // Additional utility methods
    public boolean isValidSession(String sessionId) {
        Optional<AttendanceSession> session = sessionDAO.getSession(sessionId);
        return session.isPresent() && session.get().isActive() && 
               session.get().getEndTime().isAfter(LocalDateTime.now());
    }
    
    public String getCourseFromSession(String sessionId) {
        Optional<AttendanceSession> session = sessionDAO.getSession(sessionId);
        return session.map(AttendanceSession::getCourseCode).orElse(null);
    }
}