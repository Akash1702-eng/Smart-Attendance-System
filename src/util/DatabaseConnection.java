package util;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:attendance.db";
    private static Connection connection = null;
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    
    static {
        initializeDatabase();
    }
    
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                logger.info("Database connection established successfully");
            }
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "SQLite JDBC driver not found", e);
            throw new RuntimeException("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to establish database connection", e);
            throw new RuntimeException("Failed to establish database connection", e);
        }
        return connection;
    }
    
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("PRAGMA foreign_keys = ON");
            
            String createStudentsTable = """
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT,
                    department TEXT,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            String createAttendanceTable = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL,
                    date TEXT NOT NULL,
                    time TEXT NOT NULL,
                    course_code TEXT NOT NULL,
                    status TEXT DEFAULT 'PRESENT',
                    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                    UNIQUE(student_id, date, course_code)
                )
                """;
            
            String createCoursesTable = """
                CREATE TABLE IF NOT EXISTS courses (
                    course_code TEXT PRIMARY KEY,
                    course_name TEXT NOT NULL,
                    instructor TEXT
                )
                """;
            
            String createSessionsTable = """
                CREATE TABLE IF NOT EXISTS attendance_sessions (
                    session_id TEXT PRIMARY KEY,
                    course_code TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    is_active INTEGER DEFAULT 1,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (course_code) REFERENCES courses(course_code)
                )
                """;
            
            stmt.execute(createStudentsTable);
            stmt.execute(createAttendanceTable);
            stmt.execute(createCoursesTable);
            stmt.execute(createSessionsTable);
            
            insertSampleData(conn);
            
            logger.info("Database initialized successfully");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private static void insertSampleData(Connection conn) throws SQLException {
        String insertCourses = """
            INSERT OR IGNORE INTO courses (course_code, course_name, instructor) VALUES
            ('ML2303', 'Object Oriented Programming', 'Prof. Sahebrao Bhalshankar'),
            ('ML2304', ' Digital Electronics and Microprocessor ', 'Prof. Virendrakumar Dhotre'),
            ('ML2301', 'Fundamentals of Data Structures', 'Prof. Trupti Ghongade'),
            ('ML2302', 'Database Management Systems', 'Prof. Supriya Mandhare')
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(insertCourses);
        }
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
    
    public static boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        }
    }
}
