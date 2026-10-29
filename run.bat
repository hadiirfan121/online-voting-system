@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo   VoteSecure - Online Voting System
echo ============================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on this computer.
    echo.
    echo Please install Java 17 or newer from https://adoptium.net/
    echo During installation, tick "Add to PATH", then run run.bat again.
    echo.
    pause
    exit /b 1
)

if not exist "target\VoteSecure.jar" (
    where mvn >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] VoteSecure has not been built yet and Maven is not installed.
        echo.
        echo Option 1: install Maven from https://maven.apache.org/ and run this file again.
        echo Option 2: open a terminal in this folder and run:  mvn -DskipTests package
        echo.
        pause
        exit /b 1
    )

    echo First launch detected. Building the application, this can take a minute...
    call mvn -q -DskipTests package
    if errorlevel 1 (
        echo.
        echo [ERROR] The build failed. Please check the messages above.
        echo An internet connection is required the first time so Maven can download libraries.
        echo.
        pause
        exit /b 1
    )
    echo Build completed successfully.
    echo.
)

echo Starting VoteSecure...
start "" javaw -jar "target\VoteSecure.jar"
exit /b 0
