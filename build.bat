@echo off
setlocal EnableDelayedExpansion

set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib"
set "PG_JAR=%~dp0lib\postgresql-42.7.4.jar"
set "JNA_JAR=%~dp0lib\jna-5.14.0.jar"
set "JNA_PLATFORM_JAR=%~dp0lib\jna-platform-5.14.0.jar"
set "PDFBOX_JAR=%~dp0lib\pdfbox-2.0.31.jar"
set "FONTBOX_JAR=%~dp0lib\fontbox-2.0.31.jar"
set "COMMONS_JAR=%~dp0lib\commons-logging-1.2.jar"
set "SRC=%~dp0src\javafxapplication7"
set "OUT=%~dp0out\production\JavaFXApplication7"
set "SOURCES_FILE=%TEMP%\sources_list.txt"

echo === Build ===

if not exist "%JAVAC%" (
    echo [ERROR] JDK not found: %JAVA_HOME%
    exit /b 1
)

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX not found: %JAVAFX_LIB%
    exit /b 1
)

if not exist "%PG_JAR%" (
    echo [ERROR] PostgreSQL driver missing: %PG_JAR%
    exit /b 1
)

if not exist "%OUT%" mkdir "%OUT%"

echo Collecting sources...
dir /s /b "%SRC%\*.java" > "%SOURCES_FILE%"

echo Compiling...
"%JAVAC%" ^
 --module-path "%JAVAFX_LIB%" ^
 --add-modules javafx.controls,javafx.fxml,javafx.swing ^
 -cp "%PG_JAR%;%JNA_JAR%;%JNA_PLATFORM_JAR%;%PDFBOX_JAR%;%FONTBOX_JAR%;%COMMONS_JAR%" ^
 -d "%OUT%" ^
 @"%SOURCES_FILE%"

if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo Copying resources...
xcopy /E /Y /Q "%SRC%\resources\" "%OUT%\javafxapplication7\resources\" >nul 2>&1

echo Build complete.
endlocal
