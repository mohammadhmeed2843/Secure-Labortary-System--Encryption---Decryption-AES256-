@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "PGPASSWORD=0000"
set "DB_NAME=pdfencryptionfolder"
set "DB_USER=postgres"
set "DB_HOST=localhost"

echo === Secure Medical Lab System ===
echo.

where psql >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PostgreSQL command-line tool psql was not found.
    echo Install PostgreSQL or add its bin folder to PATH.
    pause
    exit /b 1
)

echo Checking database...
psql -h %DB_HOST% -U %DB_USER% -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%DB_NAME%';" | findstr /c:"1" >nul
if errorlevel 1 (
    echo Creating database and base schema...
    psql -v ON_ERROR_STOP=1 -h %DB_HOST% -U %DB_USER% -d postgres -f setup_database.sql
    if errorlevel 1 (
        echo [ERROR] Database setup failed.
        pause
        exit /b 1
    )
) else (
    echo Database exists.
)

echo Applying migrations...
psql -v ON_ERROR_STOP=1 -h %DB_HOST% -U %DB_USER% -d %DB_NAME% -f migrate_v2.sql
if errorlevel 1 (
    echo [ERROR] migrate_v2.sql failed.
    pause
    exit /b 1
)
psql -v ON_ERROR_STOP=1 -h %DB_HOST% -U %DB_USER% -d %DB_NAME% -f migrate_v3.sql
if errorlevel 1 (
    echo [ERROR] migrate_v3.sql failed.
    pause
    exit /b 1
)
psql -v ON_ERROR_STOP=1 -h %DB_HOST% -U %DB_USER% -d %DB_NAME% -f migrate_v4.sql
if errorlevel 1 (
    echo [ERROR] migrate_v4.sql failed.
    pause
    exit /b 1
)

echo.
call "%~dp0build.bat"
if errorlevel 1 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo.
call "%~dp0run.bat"

endlocal
