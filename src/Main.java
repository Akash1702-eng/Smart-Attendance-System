import ui.MainFrame;
import util.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    private static final String APP_NAME = "Smart Attendance System";
    private static final String VERSION = "1.0";
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting " + APP_NAME + " v" + VERSION + "...");
        System.out.println("Startup Time: " + getCurrentTimestamp());
        
        // Set system look and feel for better UI consistency
        setSystemLookAndFeel();
        
        // Check and create necessary directories
        createDirectories();
        
        // Initialize database with error handling
        if (!initializeDatabase()) {
            return; // Stop if database initialization fails
        }
        
        // Create and show GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                showSplashScreen();
                
                // Create main application frame
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                
                System.out.println("✅ Application GUI loaded successfully!");
                logSystemInfo();
                
            } catch (Exception e) {
                handleFatalError("Failed to start application GUI", e);
            }
        });
    }
    
    /**
     * Set system look and feel for better native appearance
     */
    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("✅ System look and feel set successfully");
        } catch (Exception e) {
            System.err.println("⚠️  Could not set system look and feel, using default");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("❌ Failed to set any look and feel");
            }
        }
    }
    
    /**
     * Create necessary directories for the application
     */
    private static void createDirectories() {
        String[] directories = {
            "logs",
            "exports",
            "backups",
            "config"
        };
        
        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("✅ Created directory: " + dir);
                } else {
                    System.err.println("❌ Failed to create directory: " + dir);
                }
            }
        }
    }
    
    /**
     * Initialize database with proper error handling
     */
    private static boolean initializeDatabase() {
        try {
            System.out.println("🔄 Initializing database...");
            DatabaseConnection.initializeDatabase();
            System.out.println("✅ Database initialized successfully");
            return true;
            
        } catch (Exception e) {
            handleFatalError("Database initialization failed", e);
            return false;
        }
    }
    
    /**
     * Show splash screen during application startup
     */
    private static void showSplashScreen() {
        try {
            JWindow splashScreen = new JWindow();
            JPanel splashPanel = new JPanel(new BorderLayout());
            splashPanel.setBackground(new Color(0, 122, 255));
            
            // App title
            JLabel titleLabel = new JLabel(APP_NAME, JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
            
            // Version info
            JLabel versionLabel = new JLabel("Version " + VERSION, JLabel.CENTER);
            versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            versionLabel.setForeground(Color.WHITE);
            versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
            
            // Loading message
            JLabel loadingLabel = new JLabel("Loading...", JLabel.CENTER);
            loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            loadingLabel.setForeground(Color.WHITE);
            loadingLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 40, 20));
            
            splashPanel.add(titleLabel, BorderLayout.NORTH);
            splashPanel.add(versionLabel, BorderLayout.CENTER);
            splashPanel.add(loadingLabel, BorderLayout.SOUTH);
            
            splashScreen.getContentPane().add(splashPanel);
            splashScreen.setSize(400, 200);
            splashScreen.setLocationRelativeTo(null);
            splashScreen.setVisible(true);
            
            // Keep splash screen visible for 2 seconds
            Thread.sleep(2000);
            splashScreen.dispose();
            
        } catch (Exception e) {
            System.err.println("⚠️  Splash screen failed: " + e.getMessage());
            // Continue without splash screen
        }
    }
    
    /**
     * Handle fatal errors with user-friendly message
     */
    private static void handleFatalError(String message, Exception e) {
        System.err.println("❌ " + message + ": " + e.getMessage());
        e.printStackTrace();
        
        // Show error dialog to user
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                message + ":\n" + e.getMessage() + 
                "\n\nPlease check:\n" +
                "1. Database connection\n" +
                "2. Required permissions\n" +
                "3. System resources",
                "Application Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        });
    }
    
    /**
     * Log system information for debugging
     */
    private static void logSystemInfo() {
        System.out.println("=== System Information ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("User: " + System.getProperty("user.name"));
        System.out.println("Working Dir: " + System.getProperty("user.dir"));
        System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
        System.out.println("=================================");
    }
    
    /**
     * Get current timestamp for logging
     */
    private static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
    
    /**
     * Application shutdown hook for cleanup
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔴 Application shutting down...");
            System.out.println("Shutdown Time: " + getCurrentTimestamp());
            
            // Perform cleanup operations
            try {
                // Close database connections
                DatabaseConnection.closeConnection();
                System.out.println("✅ Database connections closed");
            } catch (Exception e) {
                System.err.println("❌ Error during shutdown: " + e.getMessage());
            }
        }));
    }
}
