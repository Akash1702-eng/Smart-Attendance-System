package util;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("Testing Database Connection...");
        
        if (DatabaseConnection.testConnection()) {
            System.out.println("âœ“ Database connection successful!");
            System.out.println("âœ“ Database initialized successfully!");
        } else {
            System.out.println("âœ— Database connection failed!");
        }
    }
}
