@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib"
set "PG_JAR=%~dp0lib\postgresql-42.7.4.jar"
set "JNA_JAR=%~dp0lib\jna-5.14.0.jar"
set "JNA_PLATFORM_JAR=%~dp0lib\jna-platform-5.14.0.jar"
set "PDFBOX_JAR=%~dp0lib\pdfbox-2.0.31.jar"
set "FONTBOX_JAR=%~dp0lib\fontbox-2.0.31.jar"
set "COMMONS_JAR=%~dp0lib\commons-logging-1.2.jar"
set "OUT=%~dp0out\production\JavaFXApplication7"

if not exist "%JAVA%" (
    echo [ERROR] Java runtime not found: %JAVA_HOME%
    exit /b 1
)

echo Starting application...
"%JAVA%" ^
 --module-path "%JAVAFX_LIB%" ^
 --add-modules javafx.controls,javafx.fxml,javafx.swing ^
 --enable-native-access=javafx.graphics ^
 -cp "%PG_JAR%;%JNA_JAR%;%JNA_PLATFORM_JAR%;%PDFBOX_JAR%;%FONTBOX_JAR%;%COMMONS_JAR%;%OUT%" ^
 javafxapplication7.App

endlocal