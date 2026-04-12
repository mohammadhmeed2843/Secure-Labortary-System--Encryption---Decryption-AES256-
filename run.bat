@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-24
set JAVA=%JAVA_HOME%\bin\java
set JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib
set PG_JAR=%~dp0lib\postgresql-42.7.4.jar
set OUT=%~dp0out\production\JavaFXApplication7

echo Starting JavaFX Application...
"%JAVA%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  --enable-native-access=javafx.graphics ^
  -cp "%PG_JAR%;%OUT%" ^
  javafxapplication7.JavaFXApplication7

endlocal
