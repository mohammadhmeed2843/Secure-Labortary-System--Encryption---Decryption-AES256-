@echo off
setlocal enabledelayedexpansion

:: ============================================================
::  build.bat — Compile JavaFXApplication7 (Phase 2: sub-packages)
:: ============================================================

set JAVA_HOME=C:\Program Files\Java\jdk-24
set JAVAC=%JAVA_HOME%\bin\javac
set JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib
set PG_JAR=%~dp0lib\postgresql-42.7.4.jar
set SRC_ROOT=%~dp0src
set SRC=%~dp0src\javafxapplication7
set OUT=%~dp0out\production\JavaFXApplication7
set SOURCES_FILE=%TEMP%\smls_sources.txt

echo.
echo === Secure Medical File System — Build ===
echo.

if not exist "%JAVAC%" (
    echo [ERROR] JDK not found at: %JAVA_HOME%
    pause & exit /b 1
)
if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX SDK not found at: %JAVAFX_LIB%
    pause & exit /b 1
)
if not exist "%PG_JAR%" (
    echo [ERROR] PostgreSQL JDBC driver not found: %PG_JAR%
    pause & exit /b 1
)

if not exist "%OUT%\javafxapplication7" mkdir "%OUT%\javafxapplication7"
if not exist "%OUT%\javafxapplication7\model"   mkdir "%OUT%\javafxapplication7\model"
if not exist "%OUT%\javafxapplication7\service" mkdir "%OUT%\javafxapplication7\service"
if not exist "%OUT%\javafxapplication7\session" mkdir "%OUT%\javafxapplication7\session"

:: Collect all .java files recursively into a response file
echo [1/3] Collecting sources...
dir /s /b "%SRC%\*.java" > "%SOURCES_FILE%"

echo [2/3] Compiling...
"%JAVAC%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%PG_JAR%" ^
  -d "%OUT%" ^
  @"%SOURCES_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Compilation failed.
    pause & exit /b 1
)

echo [3/3] Copying resources...
copy /Y "%SRC%\*.fxml" "%OUT%\javafxapplication7\" >nul
copy /Y "%SRC%\*.css"  "%OUT%\javafxapplication7\" >nul
if exist "%~dp0src\image" (
    if not exist "%OUT%\image" mkdir "%OUT%\image"
    copy /Y "%~dp0src\image\*.*" "%OUT%\image\" >nul 2>&1
)

echo.
echo [OK] Build successful. Run with: run.bat
echo.
endlocal
