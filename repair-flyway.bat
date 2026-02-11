@echo off
echo ========================================
echo Flyway Repair Script
echo ========================================
echo.
echo This will repair the Flyway schema history table
echo to update checksums for modified migrations.
echo.
pause

echo.
echo Running Flyway repair...
echo.

REM Load environment variables from .env file if it exists
if exist .env (
    echo Loading environment variables from .env...
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        set %%a=%%b
    )
)

REM Run Flyway repair
mvn flyway:repair -Dflyway.url=%DB_URL% -Dflyway.user=%DB_USERNAME% -Dflyway.password=%DB_PASSWORD%

echo.
echo ========================================
echo Flyway repair completed!
echo.
echo You can now start your application.
echo ========================================
pause
