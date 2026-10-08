package ui;

import model.AttendanceSession;
import model.Student;
import service.AttendanceService;
import service.WebServer;
import util.EmailSender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class MainFrame extends JFrame {

    private AttendanceService attendanceService;
    private JTabbedPane tabbedPane;
    private WebServer webServer;

    private JTextField studentIdField;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField departmentField;

    private JLabel qrCodeLabel;

    private final Color PRIMARY_COLOR = new Color(41, 128, 185);  // Blue
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);   // Green
    private final Color WARNING_COLOR = new Color(243, 156, 18);  // Orange
    private final Color DANGER_COLOR = new Color(231, 76, 60);    // Red

    public MainFrame() {
        this.attendanceService = new AttendanceService();
        this.webServer = null;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Attendance System - QR Code Based");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane.addTab("Student Management", createStudentManagementPanel());
        tabbedPane.addTab("Session Management", createSessionManagementPanel());
        tabbedPane.addTab("Reports", createReportsPanel());
        tabbedPane.addTab("Today's View", createTodayPanel());
        tabbedPane.addTab("Mobile Server", createServerPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createStudentManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Student Management - Registration & Deletion");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(PRIMARY_COLOR);
        panel.add(headerLabel, BorderLayout.NORTH);

        JTabbedPane managementTabs = new JTabbedPane();
        managementTabs.addTab("Register Student", createRegistrationForm());
        managementTabs.addTab("Delete Student", createDeletePanel());

        panel.add(managementTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRegistrationForm() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createTitledBorder("Student Information"));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        studentIdField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        departmentField = new JTextField();

        styleTextField(studentIdField);
        styleTextField(nameField);
        styleTextField(emailField);
        styleTextField(departmentField);

        formPanel.add(createFormLabel("Student ID:"));
        formPanel.add(studentIdField);
        formPanel.add(createFormLabel("Full Name:"));
        formPanel.add(nameField);
        formPanel.add(createFormLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(createFormLabel("Department:"));
        formPanel.add(departmentField);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton registerButton = createPrimaryButton("Register Student");
        JButton clearButton = createSecondaryButton("Clear Form");
        JButton refreshButton = createSecondaryButton("Refresh");

        buttonPanel.add(registerButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);

        DefaultListModel<Student> listModel = new DefaultListModel<>();
        JList<Student> studentList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(studentList);
        scrollPane.setPreferredSize(new Dimension(380, 150));

        loadStudents(listModel);

        registerButton.addActionListener(e -> {
            String studentId = studentIdField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String department = departmentField.getText().trim();

            if (studentId.isEmpty() || name.isEmpty()) {
                showMessage("Student ID and Name are required!", DANGER_COLOR);
                return;
            }

            Student student = new Student(studentId, name, email, department);
            if (attendanceService.registerStudent(student).isPresent()) {
                showMessage("Student registered successfully!", SUCCESS_COLOR);
                loadStudents(listModel);
                clearForm();
            } else {
                showMessage("Failed to register student!", DANGER_COLOR);
            }
        });

        clearButton.addActionListener(e -> clearForm());
        
        refreshButton.addActionListener(e -> loadStudents(listModel));

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDeletePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createTitledBorder("Delete Student"));

        JPanel inputPanel = new JPanel(new FlowLayout());

        JTextField deleteField = new JTextField(15);
        styleTextField(deleteField);

        JButton deleteButton = createDangerButton("Delete");
        JButton deleteAllButton = createDangerButton("Delete + Records");
        JButton refreshButton = createSecondaryButton("Refresh");

        inputPanel.add(createFormLabel("Student ID:"));
        inputPanel.add(deleteField);
        inputPanel.add(deleteButton);
        inputPanel.add(deleteAllButton);
        inputPanel.add(refreshButton);

        DefaultListModel<Student> listModel = new DefaultListModel<>();
        JList<Student> list = new JList<>(listModel);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(380, 150));

        loadStudents(listModel);

        deleteButton.addActionListener(e -> {
            String studentId = deleteField.getText().trim();

            if (studentId.isEmpty()) {
                showMessage("Enter Student ID!", WARNING_COLOR);
                return;
            }

            if (attendanceService.deleteStudent(studentId)) {
                showMessage("Deleted successfully!", SUCCESS_COLOR);
                loadStudents(listModel);
                deleteField.setText("");
            } else {
                showMessage("Cannot delete — student has records!", DANGER_COLOR);
            }
        });

        deleteAllButton.addActionListener(e -> {
            String studentId = deleteField.getText().trim();

            if (studentId.isEmpty()) {
                showMessage("Enter Student ID!", WARNING_COLOR);
                return;
            }

            if (attendanceService.deleteStudentWithAttendance(studentId)) {
                showMessage("Student + Records Deleted!", SUCCESS_COLOR);
                loadStudents(listModel);
                deleteField.setText("");
            } else {
                showMessage("Failed to delete student!", DANGER_COLOR);
            }
        });

        refreshButton.addActionListener(e -> loadStudents(listModel));

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSessionManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Classroom Session Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(380);

        splitPane.setLeftComponent(createSessionCreationPanel());
        splitPane.setRightComponent(createActiveSessionsPanel());

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSessionCreationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createTitledBorder("Create New Session"));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));

        JComboBox<String> courseCombo = new JComboBox<>();
        List<String> courses = attendanceService.getAllCourses();
        for (String c : courses) courseCombo.addItem(c);

        JTextField durationField = new JTextField("60");
        styleTextField(durationField);

        form.add(createFormLabel("Course:"));
        form.add(courseCombo);
        form.add(createFormLabel("Duration (minutes):"));
        form.add(durationField);

        JButton createBtn = createSuccessButton("Create Session and Send Attendance Link to Students");

        JPanel qrPanel = new JPanel(new BorderLayout());
        JLabel qrLabel = new JLabel("Session QR will appear here", JLabel.CENTER);
        qrLabel.setPreferredSize(new Dimension(260, 260));
        qrLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JButton saveQrButton = createPrimaryButton("Save QR Code");
        saveQrButton.setEnabled(false);
        
        qrPanel.add(qrLabel, BorderLayout.CENTER);
        qrPanel.add(saveQrButton, BorderLayout.SOUTH);

        createBtn.addActionListener(e -> {
            try {
                String courseCode = ((String) courseCombo.getSelectedItem()).split(" - ")[0];
                int duration = Integer.parseInt(durationField.getText());

                String sessionId = attendanceService.createAttendanceSession(courseCode, duration);

                if (sessionId != null) {
                    BufferedImage qr = attendanceService.generateSessionQRCode(sessionId, 250, 250);
                    if (qr != null) {
                        qrLabel.setIcon(new ImageIcon(qr));
                        qrLabel.setText("");
                        saveQrButton.setEnabled(true);
                        showMessage("Session created successfully!", SUCCESS_COLOR);
                        
                        // Store the QR image for saving
                        saveQrButton.putClientProperty("qrImage", qr);
                        saveQrButton.putClientProperty("sessionId", sessionId);
                    }

                   
                    String ipv4 = java.net.InetAddress.getLocalHost().getHostAddress();
                    String url = "https://" + ipv4 + ":8443";

                    boolean emailSent = EmailSender.sendURLToAll(url);

                    if (emailSent) {
                        showMessage("Email sent to all registered students!", SUCCESS_COLOR);
                    } else {
                        showMessage("Failed to send email to students!", DANGER_COLOR);
                    }

                } else {
                    showMessage("Could not create session!", DANGER_COLOR);
                }
            } catch (Exception ex) {
                showMessage("Invalid duration!", WARNING_COLOR);
            }
        });

        saveQrButton.addActionListener(e -> {
            BufferedImage qrImage = (BufferedImage) saveQrButton.getClientProperty("qrImage");
            String sessionId = (String) saveQrButton.getClientProperty("sessionId");
            
            if (qrImage != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save QR Code");
                fileChooser.setSelectedFile(new File("session_qr_" + sessionId + ".png"));
                
                int userSelection = fileChooser.showSaveDialog(this);
                
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    try {
                        javax.imageio.ImageIO.write(qrImage, "PNG", fileToSave);
                        showMessage("QR Code saved successfully!", SUCCESS_COLOR);
                    } catch (Exception ex) {
                        showMessage("Failed to save QR Code: " + ex.getMessage(), DANGER_COLOR);
                    }
                }
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(createBtn, BorderLayout.CENTER);
        panel.add(qrPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createActiveSessionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createTitledBorder("Active Sessions"));

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> sessionList = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(sessionList);

        JButton refreshBtn = createSecondaryButton("Refresh");
        JButton endBtn = createDangerButton("End Session");

        refreshBtn.addActionListener(e -> refreshSessions(model));

        endBtn.addActionListener(e -> {
            String selected = sessionList.getSelectedValue();
            if (selected != null) {
                String sessionId = selected.split(" - ")[0];
                if (attendanceService.deactivateSession(sessionId)) {
                    showMessage("Session ended!", SUCCESS_COLOR);
                    refreshSessions(model);
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(refreshBtn);
        btnPanel.add(endBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        refreshSessions(model);
        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Attendance Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JTextArea reportArea = new JTextArea(20, 60);
        reportArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(reportArea);

        JPanel searchPanel = new JPanel(new FlowLayout());

        JTextField searchField = new JTextField(15);
        JButton searchBtn = createPrimaryButton("Search Student");
        JButton allBtn = createSecondaryButton("Show All");

        searchPanel.add(new JLabel("Student ID:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(allBtn);

        searchBtn.addActionListener(e -> {
            String id = searchField.getText().trim();

            if (id.isEmpty()) {
                showMessage("Enter Student ID!", WARNING_COLOR);
                return;
            }

            attendanceService.getStudent(id).ifPresentOrElse(
                    student -> {
                        reportArea.setText("Attendance for: " + student.getName() + "\n");
                        reportArea.append("ID: " + id + "\n\n");

                        List<model.Attendance> rec = attendanceService.getStudentAttendance(id);

                        if (rec.isEmpty()) {
                            reportArea.append("No attendance records.\n");
                        } else {
                            reportArea.append("Date | Time | Course\n");
                            rec.forEach(a -> {
                                reportArea.append(a.getDate() + " | " + a.getTime() + " | " + a.getCourseCode() + "\n");
                            });
                        }
                    },
                    () -> reportArea.setText("Student not found.")
            );
        });

        allBtn.addActionListener(e -> {
            List<Student> all = attendanceService.getAllStudents();
            reportArea.setText("All Students:\n\n");

            all.forEach(s -> reportArea.append(s.getStudentId() + " - " + s.getName() + "\n"));
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTodayPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Today's Attendance - " + LocalDate.now());
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JTextArea todayArea = new JTextArea(20, 60);
        todayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(todayArea);

        JButton refreshBtn = createPrimaryButton("Refresh");

        refreshBtn.addActionListener(e -> {
            List<model.Attendance> list = attendanceService.getTodayAttendance();

            todayArea.setText("Today's Attendance (" + LocalDate.now() + ")\n\n");

            if (list.isEmpty()) {
                todayArea.append("No attendance today.\n");
            } else {
                list.forEach(a -> {
                    attendanceService.getStudent(a.getStudentId()).ifPresent(st ->
                            todayArea.append(a.getTime() + " - " + st.getName() +
                                    " - " + a.getCourseCode() + "\n")
                    );
                });
            }
        });

        refreshBtn.doClick();

        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Mobile Server Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JPanel config = new JPanel(new GridLayout(4, 1, 10, 10));
        config.setBorder(createTitledBorder("Server Settings"));

        JLabel statusLabel = new JLabel("Server Status: Stopped", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(DANGER_COLOR);

        JTextField portField = new JTextField("8080");
        styleTextField(portField);

        JButton startBtn = createSuccessButton("Start Server");
        JButton stopBtn = createDangerButton("Stop Server");
        JButton mobileBtn = createPrimaryButton("Launch Mobile App");
        JButton openBrowserBtn = createPrimaryButton("Open Mobile Scanner");

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(startBtn);
        btnPanel.add(stopBtn);
        btnPanel.add(mobileBtn);
        // btnPanel.add(openBrowserBtn);

        JTextArea infoArea = new JTextArea(8, 50);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        infoArea.setText("Server not started.\n\n" +
                "To use mobile camera:\n" +
                "1. Start the server\n" +
                "2. Use HTTPS URL on mobile devices\n" +
                "3. Allow camera permissions when prompted");

        JScrollPane infoScroll = new JScrollPane(infoArea);

        startBtn.addActionListener(e -> {
            try {
                int port = Integer.parseInt(portField.getText());

                webServer = new WebServer(attendanceService);
                webServer.start(port);

                statusLabel.setText("Server Running on ports " + port + " (HTTP) & 8443 (HTTPS)");
                statusLabel.setForeground(SUCCESS_COLOR);

                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);

                try {
                    String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                    infoArea.setText("**** Server Running Successfully ****\n\n" +
                            "----- Mobile Access Instructions -----\n" +
                            "1. On mobile, open: https://" + ip + ":8443\n" +
                            "2. Allow camera permissions when prompted\n" +
                            "3. Enter Student ID and scan QR code\n\n" +
                            "First time HTTPS access will show security warning.\n" +
                            "   Click 'Advanced' → 'Proceed to site'");
                } catch (Exception ignored) {
                    infoArea.setText("**** Server Running ****\n" +
                            "HTTP: localhost:" + port + "\n" +
                            "HTTPS: localhost:8443\n\n" +
                            "Use HTTPS URL for mobile camera access");
                }

            } catch (Exception ex) {
                showMessage("Could not start server: " + ex.getMessage(), DANGER_COLOR);
            }
        });

        stopBtn.addActionListener(e -> {
            if (webServer != null) {
                webServer.stop();
                statusLabel.setText("Server Status: Stopped");
                statusLabel.setForeground(DANGER_COLOR);
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                infoArea.setText("Server stopped.\nStart server to enable mobile access.");
            }
        });

        mobileBtn.addActionListener(e -> launchMobileApp());

        openBrowserBtn.addActionListener(e -> {
            if (webServer != null && webServer.isRunning()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://localhost:8443"));
                } catch (Exception ex) {
                    showMessage("Cannot open browser: " + ex.getMessage(), WARNING_COLOR);
                }
            } else {
                showMessage("Start the server first!", WARNING_COLOR);
            }
        });

        config.add(statusLabel);
        config.add(new JLabel("HTTP Port:"));
        config.add(portField);
        config.add(btnPanel);

        panel.add(config, BorderLayout.NORTH);
        panel.add(infoScroll, BorderLayout.CENTER);

        return panel;
    }

    private void refreshSessions(DefaultListModel<String> model) {
        model.clear();
        List<AttendanceSession> sessions = attendanceService.getActiveSessions();
        sessions.forEach(s -> model.addElement(
                s.getSessionId() + " - " + s.getCourseCode() +
                        " (Until: " + s.getEndTime().toLocalTime() + ")"
        ));
    }

    private void launchMobileApp() {
        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

            String classpath =
                    "bin" + File.pathSeparator +
                    "sqlite-jdbc-3.36.0.3.jar" + File.pathSeparator +
                    "lib" + File.separator + "core-3.4.1.jar" + File.pathSeparator +
                    "lib" + File.separator + "javase-3.4.1.jar" + File.pathSeparator +
                    "lib" + File.separator + "webcam-capture-0.3.12.jar" + File.pathSeparator +
                    "lib" + File.separator + "bridj-0.7.0.jar" + File.pathSeparator +
                    "lib" + File.separator + "slf4j-api-1.7.25.jar";

            ProcessBuilder builder = new ProcessBuilder(
                    javaBin, "-cp", classpath, "mobile.StudentMobileApp"
            );

            builder.start();
            showMessage("Mobile App Launched!", SUCCESS_COLOR);

        } catch (Exception e) {
            showMessage("Failed to launch mobile app: " + e.getMessage(), DANGER_COLOR);
        }
    }

    // ----------------------------- BUTTON STYLES -----------------------------

    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setBackground(PRIMARY_COLOR);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setBackground(new Color(100, 100, 100));
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JButton createSuccessButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setBackground(SUCCESS_COLOR);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JButton createDangerButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setBackground(DANGER_COLOR);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                title
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(PRIMARY_COLOR);
        return border;
    }

    private void showMessage(String msg, Color color) {
        JOptionPane.showMessageDialog(this, msg, "System Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadStudents(DefaultListModel<Student> listModel) {
        listModel.clear();
        attendanceService.getAllStudents().forEach(listModel::addElement);
    }

    private void clearForm() {
        studentIdField.setText("");
        nameField.setText("");
        emailField.setText("");
        departmentField.setText("");
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainFrame().setVisible(true);
        });
    }
}