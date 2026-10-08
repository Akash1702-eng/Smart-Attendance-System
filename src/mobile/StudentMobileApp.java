package mobile;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.Dimension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

// === Webcam + ZXing QR Scanner ===
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

public class StudentMobileApp extends JFrame {

    private JTextField studentIdField;
    private JTextField qrDataField;
    private JTextArea resultArea;
    private JButton scanButton;
    private JButton uploadButton;
    private JButton submitButton; // hidden
    private JLabel statusLabel;

    private String serverUrl = "http://localhost:8080";

    private final Color PRIMARY_COLOR  = new Color(0, 122, 255);
    private final Color SUCCESS_COLOR  = new Color(52, 199, 89);
    private final Color DANGER_COLOR   = new Color(255, 59, 48);
    private final Color WARNING_COLOR  = new Color(255, 149, 0);

    private final Color BACKGROUND_COLOR = Color.WHITE;
    private final Color CARD_COLOR = Color.WHITE;
    private final Color TEXT_COLOR = Color.BLACK;

    public StudentMobileApp() {
        initializeUI();
        startStatusChecker();
    }

    private void initializeUI() {

        setTitle("Student Attendance - Mobile");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 680);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10,10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        mainPanel.setBackground(BACKGROUND_COLOR);

        JLabel headerLabel = new JLabel("Student Attendance");
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(PRIMARY_COLOR);
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(BACKGROUND_COLOR);

        JPanel idPanel = new JPanel(new BorderLayout(10,5));
        idPanel.setBackground(BACKGROUND_COLOR);

        JLabel idLabel = new JLabel("Student ID:");
        idLabel.setFont(new Font("Arial", Font.BOLD, 16));

        studentIdField = new JTextField();
        styleTextField(studentIdField);

        idPanel.add(idLabel, BorderLayout.NORTH);
        idPanel.add(studentIdField, BorderLayout.CENTER);

        // QR TEXT FIELD
        JPanel qrPanel = new JPanel(new BorderLayout(10,5));
        qrPanel.setBackground(BACKGROUND_COLOR);

        JLabel qrLabel = new JLabel("QR Code Data:");
        qrLabel.setFont(new Font("Arial", Font.BOLD, 16));

        qrDataField = new JTextField();
        styleTextField(qrDataField);

        qrPanel.add(qrLabel, BorderLayout.NORTH);
        qrPanel.add(qrDataField, BorderLayout.CENTER);

        // BUTTON PANEL
        JPanel buttonPanel = new JPanel(new GridLayout(1,3,10,10));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        scanButton = createSimpleButton("Scan QR", PRIMARY_COLOR);
        uploadButton = createSimpleButton("Upload QR", WARNING_COLOR);
        submitButton = createSimpleButton("Mark Attendance", SUCCESS_COLOR);
        submitButton.setVisible(false);

        buttonPanel.add(scanButton);
        buttonPanel.add(uploadButton);
        buttonPanel.add(submitButton);

        // STATUS LABEL
        statusLabel = new JLabel("Checking server...");
        statusLabel.setForeground(WARNING_COLOR);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // RESULT AREA
        resultArea = new JTextArea(8,30);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("Result"));

        JTextArea instructions = new JTextArea(
            "Instructions:\n\n" +
            "1. Enter Student ID\n" +
            "2. Scan QR using webcam OR upload QR photo\n" +
            "3. Attendance will be marked automatically\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240,240,240));

        // Add listeners
        scanButton.addActionListener(e -> showQRScannerDialog());
        uploadButton.addActionListener(e -> uploadQRImage());
        qrDataField.addActionListener(e -> autoMarkAttendance());

        formPanel.add(idPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(qrPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(buttonPanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(statusLabel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(resultScroll);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(instructions);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    // -------------------------------------------------------------------------
    // NEW FEATURE: UPLOAD QR IMAGE
    // -------------------------------------------------------------------------
    private void uploadQRImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select QR Image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files", "png", "jpg", "jpeg", "webp"));

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    showMessage("Invalid image!", DANGER_COLOR);
                    return;
                }

                LuminanceSource src = new BufferedImageLuminanceSource(img);
                BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(src));

                Result qrResult = new MultiFormatReader().decode(bmp);
                String qrText = qrResult.getText();

                qrDataField.setText(fixQRFormat(qrText));

                autoMarkAttendance();

            } catch (NotFoundException nf) {
                showMessage("QR not found in image!", DANGER_COLOR);

            } catch (Exception ex) {
                showMessage("Error reading image: " + ex.getMessage(), DANGER_COLOR);
            }
        }
    }

    // -------------------------------------------------------------------------
    // QR SCANNER USING WEBCAM
    // -------------------------------------------------------------------------
    private void showQRScannerDialog() {

        JDialog dialog = new JDialog(this, "Scan QR Code", true);
        dialog.setSize(480, 360);
        dialog.setLocationRelativeTo(this);

        Webcam webcam;

        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                showMessage("No webcam found!", DANGER_COLOR);
                return;
            }
        } catch (Exception ex) {
            showMessage("Webcam error: " + ex.getMessage(), DANGER_COLOR);
            return;
        }

        webcam.setViewSize(new Dimension(640, 480));

        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setMirrored(true);

        dialog.add(panel);

        new Thread(() -> {
            try {
                while (dialog.isVisible()) {
                    if (webcam.isOpen()) {
                        BufferedImage buff = webcam.getImage();

                        if (buff != null) {
                            try {
                                LuminanceSource src = new BufferedImageLuminanceSource(buff);
                                BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(src));

                                Result qrResult = new MultiFormatReader().decode(bmp);

                                SwingUtilities.invokeLater(() -> {
                                    qrDataField.setText(fixQRFormat(qrResult.getText()));
                                    webcam.close();
                                    dialog.dispose();
                                    autoMarkAttendance();
                                });

                                break;

                            } catch (NotFoundException ignored) {}
                        }
                    }
                    Thread.sleep(120);
                }
            } catch (Exception ignored) {}
        }).start();

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                webcam.close();
            }
        });

        dialog.setVisible(true);
    }

    // -------------------------------------------------------------------------
    private String fixQRFormat(String raw) {
        raw = raw.trim();
        if (!raw.contains(":")) raw = "SESSION:" + raw;
        return raw;
    }

    // -------------------------------------------------------------------------
    private void autoMarkAttendance() {

        String studentId = studentIdField.getText().trim();
        String qrData = qrDataField.getText().trim();

        if (studentId.isEmpty()) {
            showMessage("Enter Student ID first!", WARNING_COLOR);
            return;
        }

        if (qrData.isEmpty()) {
            showMessage("Invalid QR!", DANGER_COLOR);
            return;
        }

        showResult("Submitting attendance…", PRIMARY_COLOR);

        new Thread(() -> {
            String response = sendAttendanceRequest(studentId, qrData);

            SwingUtilities.invokeLater(() -> {
                if (response.toLowerCase().contains("success"))
                    showResult("SUCCESS — Attendance marked!", SUCCESS_COLOR);
                else
                    showResult("ERROR: " + response, DANGER_COLOR);
            });

        }).start();
    }

    // -------------------------------------------------------------------------
    private String sendAttendanceRequest(String studentId, String qrData) {

        try {
            URL url = new URL(serverUrl + "/attendance");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body =
                "studentId=" + URLEncoder.encode(studentId, "UTF-8") +
                "&qrData=" + URLEncoder.encode(qrData, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                status == 200 ? conn.getInputStream() : conn.getErrorStream()
            ));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) sb.append(line);

            return sb.toString();

        } catch (Exception e) {
            return "Network Error: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    private void startStatusChecker() {
        Timer t = new Timer(8000, e -> checkServer());
        t.start();
        checkServer();
    }

    private void checkServer() {

        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                int code = conn.getResponseCode();

                SwingUtilities.invokeLater(() -> {
                    if (code == 200) {
                        statusLabel.setText("Online");
                        statusLabel.setForeground(SUCCESS_COLOR);
                    } else {
                        statusLabel.setText("Server Error");
                        statusLabel.setForeground(WARNING_COLOR);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Offline");
                    statusLabel.setForeground(DANGER_COLOR);
                });
            }

        }).start();
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(10,10,10,10)
        ));
    }

    private void showResult(String msg, Color color) {
        resultArea.setText(msg);
        resultArea.setForeground(color);
    }

    private void showMessage(String msg, Color color) {
        JOptionPane.showMessageDialog(this, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private class MobileButton extends JButton {
        private final Color normal, hover, pressed;

        public MobileButton(String text, Color bg) {
            super(text);
            this.normal = bg;
            this.hover = bg.darker();
            this.pressed = bg.darker().darker();

            setUI(new BasicButtonUI());
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 14));
            setBorder(BorderFactory.createEmptyBorder(10,15,10,15));
            setFocusPainted(false);
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(
                getModel().isPressed() ? pressed :
                getModel().isRollover() ? hover : normal
            );
            g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
            g2.dispose();
            super.paintComponent(g);
        }
        @Override public boolean isOpaque() { return false; }
    }

    private JButton createSimpleButton(String text, Color bg) {
        return new MobileButton(text, bg);
    }

    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new StudentMobileApp().setVisible(true));
    }
}
