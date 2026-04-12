@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-24
set JAVAC=%JAVA_HOME%\bin\javac
set JAVA=%JAVA_HOME%\bin\java
set JAVAFX_LIB=%~dp0lib\javafx-sdk-24\lib
set PG_JAR=%~dp0lib\postgresql-42.7.4.jar
set SRC=%~dp0src\javafxapplication7
set OUT=%~dp0out\production\JavaFXApplication7

echo === Compiling JavaFXApplication7 ===

if not exist "%OUT%\javafxapplication7" mkdir "%OUT%\javafxapplication7"

"%JAVAC%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%PG_JAR%" ^
  -d "%OUT%" ^
  "%SRC%\*.java"

if %ERRORLEVEL% NEQ 0 (
    echo Compilation FAILED.
    exit /b 1
)

echo === Copying resources ===
copy "%SRC%\*.fxml" "%OUT%\javafxapplication7\" >nul 2>&1
copy "%SRC%\*.css"  "%OUT%\javafxapplication7\" >nul 2>&1

if not exist "%OUT%\image" mkdir "%OUT%\image"
copy "%~dp0src\image\*.*" "%OUT%\image\" >nul 2>&1

echo === Build successful ===
echo Run the app with:  run.bat

endlocal
