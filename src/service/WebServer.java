package service;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import model.Student;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.Optional;

public class WebServer {


    private HttpServer httpServer;


    private HttpsServer httpsServer;

    private final AttendanceService attendanceService;
    private boolean isRunning = false;

    public WebServer(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }


    public void start(int httpPort) throws IOException {


        httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
        httpServer.createContext("/attendance", new AttendanceHandler());
        httpServer.createContext("/status", new StatusHandler());
        httpServer.createContext("/student", new StudentHandler());

        httpServer.setExecutor(null);
        httpServer.start();

        System.out.println("📡 HTTP Smart Attendance Server on port " + httpPort);


        try {
            int httpsPort = 8443;
            

            File keystoreFile = new File("server.jks");
            if (!keystoreFile.exists()) {
                System.out.println("🔑 Generating self-signed certificate...");
                generateSelfSignedCertificate();
            }


            char[] password = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream("server.jks")) {
                ks.load(fis, password);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);


            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            httpsServer = HttpsServer.create(new InetSocketAddress(httpsPort), 0);

            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        
                        SSLParameters sslParams = context.getDefaultSSLParameters();
                        params.setSSLParameters(sslParams);
                    } catch (Exception ex) {
                        System.err.println("Error configuring HTTPS: " + ex.getMessage());
                    }
                }
            });

            // Mobile QR page is only on HTTPS
            httpsServer.createContext("/", new MobileQRPageHandler());
            httpsServer.createContext("/scanner", new MobileQRPageHandler()); // Alternative endpoint

            // Also expose same JSON APIs over HTTPS
            httpsServer.createContext("/attendance", new AttendanceHandler());
            httpsServer.createContext("/status", new StatusHandler());
            httpsServer.createContext("/student", new StudentHandler());

            httpsServer.setExecutor(null);
            httpsServer.start();

            System.out.println("🔐 HTTPS Smart Attendance Server on port " + httpsPort);
            System.out.println("   Mobile Scanner: https://<YOUR-IP>:" + httpsPort + "/");
            System.out.println("   Local Access: https://localhost:" + httpsPort + "/");

        } catch (Exception e) {
            System.err.println("❌ Failed to start HTTPS server: " + e.getMessage());
            System.err.println("📝 HTTP-only mode active. Mobile camera may not work.");
        }

        isRunning = true;
    }


    private void generateSelfSignedCertificate() throws Exception {
        String[] command = {
            "keytool", "-genkey",
            "-alias", "smartattendance",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "365",
            "-keystore", "server.jks",
            "-dname", "CN=Smart Attendance, OU=Development, O=SmartAttendance, L=City, ST=State, C=US",
            "-storepass", "password",
            "-keypass", "password",
            "-noprompt"
        };
        
        Process process = Runtime.getRuntime().exec(command);
        int result = process.waitFor();
        
        if (result == 0 && new File("server.jks").exists()) {
            System.out.println("✅ Self-signed certificate created successfully");
        } else {
            // Try alternative approach
            System.out.println("🔄 Trying alternative certificate generation...");
            generateCertificateWithKeytool();
        }
    }

    // Alternative certificate generation
    private void generateCertificateWithKeytool() throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, "password".toCharArray());
            
            // Save empty keystore first
            try (FileOutputStream fos = new FileOutputStream("server.jks")) {
                keyStore.store(fos, "password".toCharArray());
            }
            
            System.out.println("✅ Keystore created. Please run this command manually:");
            System.out.println("keytool -genkey -alias smartattendance -keyalg RSA -keysize 2048 -validity 365 -keystore server.jks -storepass password -keypass password -dname \"CN=SmartAttendance\"");
            
        } catch (Exception e) {
            throw new Exception("Certificate generation failed: " + e.getMessage());
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (httpsServer != null) {
            httpsServer.stop(0);
        }
        isRunning = false;
        System.out.println("Smart Attendance Server stopped.");
    }

    public boolean isRunning() {
        return isRunning;
    }


    class MobileQRPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Only GET allowed\"}");
                return;
            }

            String html = generateScannerHTML();

            // Add security headers that help with camera access
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            ex.getResponseHeaders().set("Pragma", "no-cache");
            ex.getResponseHeaders().set("Expires", "0");
            
            // Important for HTTPS sites
            ex.getResponseHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }


    private String generateScannerHTML() {
        return """
    <!DOCTYPE html>
    <html>
    <head>
        <title>QR Scanner</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <script src="https://unpkg.com/html5-qrcode"></script>
        <style>
            body { margin: 20px; font-family: Arial; background: #fff; }
            .box { margin: 10px 0; padding: 10px; border: 1px solid #ccc; border-radius: 5px; }
            input { width: 100%; padding: 10px; margin: 5px 0; border: 1px solid #ccc; border-radius: 3px; }
            button { width: 100%; padding: 12px; margin: 5px 0; background: #007bff; color: white; border: none; border-radius: 3px; cursor: pointer; }
            button.stop { background: #dc3545; }
            #result { padding: 10px; margin: 10px 0; border-radius: 3px; text-align: center; }
            .success { background: #d4edda; color: #155724; }
            .error { background: #f8d7da; color: #721c24; }
            #status { padding: 8px; margin: 5px 0; text-align: center; background: #e9ecef; border-radius: 3px; }
        </style>
    </head>
    <body>
        <h2>QR Attendance</h2>
        
        <div class="box">
            <input type="text" id="studentId" placeholder="Student ID">
        </div>

        <div id="status">Enter ID and click Start Camera</div>

        <button onclick="startCamera()">Start Camera</button>
        <button onclick="stopCamera()" class="stop">Stop Camera</button>

        <div id="qr-reader"></div>
        
        <div id="result"></div>

        <script>
            let scanner = null;
            let scanning = false;

            function updateStatus(msg) {
                document.getElementById('status').textContent = msg;
            }

            function showResult(msg, type) {
                const el = document.getElementById('result');
                el.textContent = msg;
                el.className = type;
            }

            async function startCamera() {
                const id = document.getElementById('studentId').value.trim();
                if (!id) {
                    alert('Enter Student ID');
                    return;
                }

                updateStatus('Starting camera...');
                
                try {
                    if (scanner && scanning) {
                        await scanner.clear();
                    }

                    scanner = new Html5QrcodeScanner("qr-reader", { fps: 10, qrbox: 250 }, false);
                    scanner.render(onScan, () => {});
                    scanning = true;
                    updateStatus('Camera ready - Scan QR code');
                    
                } catch (err) {
                    updateStatus('Camera error');
                    if (err.message.includes('permission')) {
                        showResult('Allow camera access', 'error');
                    } else {
                        showResult('Camera error', 'error');
                    }
                }
            }

            async function stopCamera() {
                if (scanner && scanning) {
                    await scanner.clear();
                    scanning = false;
                    updateStatus('Camera stopped');
                    showResult('', '');
                }
            }

            function onScan(text) {
                const id = document.getElementById('studentId').value.trim();
                if (!id) return;

                updateStatus('Sending...');
                if (scanner && scanning) {
                    scanner.clear();
                    scanning = false;
                }

                if (!text.includes(':')) {
                    text = 'SESSION:' + text;
                }

                const data = new URLSearchParams();
                data.append('studentId', id);
                data.append('qrData', text);

                fetch('/attendance', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: data
                })
                .then(r => r.json())
                .then(result => {
                    if (result.status === "success") {
                        showResult('✓ ' + result.message, 'success');
                        updateStatus('Success');
                    } else {
                        showResult('✗ ' + result.message, 'error');
                        updateStatus('Failed');
                    }
                    setTimeout(() => { if (!scanning) startCamera(); }, 2000);
                })
                .catch(err => {
                    showResult('Network error', 'error');
                    updateStatus('Error');
                    setTimeout(() => { if (!scanning) startCamera(); }, 2000);
                });
            }

            document.getElementById('studentId').focus();
            document.getElementById('studentId').onkeypress = function(e) {
                if (e.key === 'Enter') startCamera();
            };
        </script>
    </body>
    </html>
    """;
    }


    class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String json = "{\"status\":\"running\",\"time\":\"" + LocalDateTime.now() + "\",\"https\":\"" + (httpsServer != null) + "\"}";
            sendJson(ex, 200, json);
        }
    }


    class StudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Only GET allowed\"}");
                return;
            }

            String query = ex.getRequestURI().getQuery();
            String id = getParameter(query, "id");

            Optional<Student> st = attendanceService.getStudent(id);

            if (st.isEmpty()) {
                sendJson(ex, 404, "{\"error\":\"Student not found\"}");
                return;
            }

            Student s = st.get();
            String json = String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"department\":\"%s\"}",
                    s.getStudentId(), s.getName(), s.getEmail(), s.getDepartment()
            );

            sendJson(ex, 200, json);
        }
    }


    class AttendanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Only POST allowed\"}");
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            body = java.net.URLDecoder.decode(body, StandardCharsets.UTF_8);

            String studentId = null;
            String qrData = null;

            for (String p : body.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("studentId".equals(kv[0])) studentId = kv[1];
                    if ("qrData".equals(kv[0])) qrData = kv[1];
                }
            }

            if (studentId == null || qrData == null || studentId.isEmpty() || qrData.isEmpty()) {
                sendJson(ex, 400, "{\"status\":\"error\",\"message\":\"Missing studentId or qrData fields\"}");
                return;
            }

            // Fix QR prefix if needed
            if (!qrData.startsWith("SESSION:") && !qrData.startsWith("SESSION_")
                    && !qrData.startsWith("ATTENDANCE_SESSION:")) {
                qrData = "SESSION:" + qrData;
            }

            boolean ok = attendanceService.markAttendanceFromQR(studentId, qrData);

            if (ok)
                sendJson(ex, 200, "{\"status\":\"success\",\"message\":\"Attendance marked successfully\"}");
            else
                sendJson(ex, 400, "{\"status\":\"error\",\"message\":\"Invalid QR code or Student ID\"}");
        }
    }


    private void sendJson(HttpExchange ex, int status, String response) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getParameter(String query, String key) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }
}