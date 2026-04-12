@echo off
setlocal

:: ============================================================
::  build.bat — Compile JavaFXApplication7 from source
::  Usage: double-click or run from any terminal
:: ============================================================

set JAVA_HOME=C:\Program Files\Java\jdk-24
set JAVAC=%JAVA_HOME%\bin\javac
set JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib
set PG_JAR=%~dp0lib\postgresql-42.7.4.jar
set SRC=%~dp0src\javafxapplication7
set OUT=%~dp0out\production\JavaFXApplication7

echo.
echo === Secure Medical File System — Build ===
echo.

if not exist "%JAVAC%" (
    echo [ERROR] JDK not found at: %JAVA_HOME%
    echo         Update JAVA_HOME in this script and try again.
    pause & exit /b 1
)

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX SDK not found at: %JAVAFX_LIB%
    echo         Run the project setup steps in README first.
    pause & exit /b 1
)

if not exist "%PG_JAR%" (
    echo [ERROR] PostgreSQL JDBC driver not found: %PG_JAR%
    pause & exit /b 1
)

if not exist "%OUT%\javafxapplication7" mkdir "%OUT%\javafxapplication7"

echo [1/3] Compiling Java sources...
"%JAVAC%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%PG_JAR%" ^
  -d "%OUT%" ^
  "%SRC%\*.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Compilation failed. Fix errors above and retry.
    pause & exit /b 1
)

echo [2/3] Copying FXML and CSS resources...
copy /Y "%SRC%\*.fxml" "%OUT%\javafxapplication7\" >/dev/null
copy /Y "%SRC%\*.css"  "%OUT%\javafxapplication7\" >/dev/null

echo [3/3] Copying image assets...
if not exist "%OUT%\image" mkdir "%OUT%\image"
copy /Y "%~dp0src\image\*.*" "%OUT%\image\" >/dev/null 2>&1

echo.
echo [OK] Build successful. Run with: run.bat
echo.
endlocal
