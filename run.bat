@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib"
set "PG_JAR=%~dp0lib\postgresql-42.7.4.jar"
set "OUT=%~dp0out\production\JavaFXApplication7"

if not exist "%JAVA%" (
    echo [ERROR] Java runtime not found: %JAVA_HOME%
    exit /b 1
)

echo Starting application...
"%JAVA%" ^
 --module-path "%JAVAFX_LIB%" ^
 --add-modules javafx.controls,javafx.fxml ^
 --enable-native-access=javafx.graphics ^
 -cp "%PG_JAR%;%OUT%" ^
 javafxapplication7.App

endlocal