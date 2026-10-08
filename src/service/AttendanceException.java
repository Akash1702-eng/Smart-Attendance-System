package service;

public class AttendanceException extends Exception {
    public AttendanceException(String message) {
        super(message);
    }
    
    public AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
