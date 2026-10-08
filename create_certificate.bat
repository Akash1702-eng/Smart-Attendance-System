@echo off
echo Creating SSL Certificate for Smart Attendance System...
echo.

keytool -genkey -alias smartattendance -keyalg RSA -keysize 2048 -validity 365 -keystore server.jks -storepass password -keypass password -dname "CN=SmartAttendance, OU=Development, O=SmartAttendance, L=City, ST=State, C=US" -noprompt

if exist server.jks (
    echo ✅ Certificate created successfully!
    echo 📁 File: server.jks
    echo 🔑 Password: password
    echo.
    echo You can now run the server with HTTPS support.
) else (
    echo ❌ Failed to create certificate.
    echo Please install Java and ensure keytool is in your PATH.
)

pause