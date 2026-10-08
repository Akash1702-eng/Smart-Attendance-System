@echo off
title Smart Attendance System Runner
echo ==========================================
echo        SMART ATTENDANCE SYSTEM
echo ==========================================
echo.

if not exist "bin\Main.class" (
    echo ERROR: Program not compiled!
    pause
    exit /b
)

:menu
cls
echo 1. Run Main Program
echo 2. Run Mobile App
echo 3. Exit
echo.
set /p choice=Select 1-3: 

if "%choice%"=="1" goto runMain
if "%choice%"=="2" goto runMobile
if "%choice%"=="3" exit /b
goto menu

:runMain
cls
echo Starting Main Desktop App...
java -cp "bin;sqlite-jdbc-3.36.0.3.jar;lib\core-3.4.1.jar;lib\javase-3.4.1.jar;lib\jakarta.mail-2.0.1.jar;lib\jakarta.activation-2.0.1.jar" Main
pause
goto menu

:runMobile
cls
echo Starting Student Mobile App...
java -cp "bin;sqlite-jdbc-3.36.0.3.jar;lib\webcam-capture-0.3.12.jar;lib\bridj-0.7.0.jar;lib\slf4j-api-1.7.25.jar;lib\core-3.4.1.jar;lib\javase-3.4.1.jar;lib\jakarta.mail-2.0.1.jar;lib\jakarta.activation-2.0.1.jar" mobile.StudentMobileApp
pause
goto menu
