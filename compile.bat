@echo off
title Compiling Smart Attendance System
echo ========================================
echo    SMART ATTENDANCE SYSTEM COMPILER
echo ========================================
echo.

:: Create directories
if not exist "bin" mkdir bin
if not exist "lib" mkdir lib

echo Checking required libraries...

if not exist "sqlite-jdbc-3.36.0.3.jar" (
    echo MISSING: sqlite-jdbc-3.36.0.3.jar
    pause
    exit /b
)

if not exist "lib\jakarta.mail-2.0.1.jar" (
    echo MISSING: jakarta.mail-2.0.1.jar
    pause
    exit /b
)

if not exist "lib\jakarta.activation-2.0.1.jar" (
    echo MISSING: jakarta.activation-2.0.1.jar
    pause
    exit /b
)

if not exist "lib\core-3.4.1.jar" (
    echo MISSING: ZXing core
    pause
    exit /b
)

if not exist "lib\javase-3.4.1.jar" (
    echo MISSING: ZXing javase
    pause
    exit /b
)

:: FINAL CLASS PATH WITH MAIL SUPPORT
set CP=.;sqlite-jdbc-3.36.0.3.jar;lib\core-3.4.1.jar;lib\javase-3.4.1.jar;lib\webcam-capture-0.3.12.jar;lib\bridj-0.7.0.jar;lib\slf4j-api-1.7.25.jar;lib\jakarta.mail-2.0.1.jar;lib\jakarta.activation-2.0.1.jar;

echo Cleaning old classes...
del /q bin\*.class 2>nul
del /q bin\dao\*.class 2>nul
del /q bin\model\*.class 2>nul
del /q bin\service\*.class 2>nul
del /q bin\ui\*.class 2>nul
del /q bin\util\*.class 2>nul
del /q bin\mobile\*.class 2>nul

echo.
echo Compiling all project files...
javac -cp "%CP%" -d bin src\*.java src\dao\*.java src\model\*.java src\util\*.java src\service\*.java src\ui\*.java src\mobile\*.java

if %errorlevel% neq 0 (
    echo.
    echo COMPILATION FAILED!
    echo Check the above errors.
    pause
    exit /b
)

echo.
echo ========================================
echo   COMPILATION SUCCESSFUL!
echo ========================================
echo.
pause
exit /b
