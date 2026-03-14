@echo off
:: Start all dev servers: 3 Goliath instances + Velocity proxy
:: Each server opens in its own terminal window

echo === Starting GtolProxy Dev Environment ===

set DEV_DIR=%~dp0..\dev
set JAVA_OPTS=-Xms512M -Xmx1G

:: Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found! Install JDK 21+
    pause
    exit /b 1
)

:: Start Goliath instances
for %%i in (1 2 3) do (
    if not exist "%DEV_DIR%\goliath-%%i\server.jar" (
        echo ERROR: goliath-%%i not set up. Run setup-dev.bat first!
        pause
        exit /b 1
    )
    echo Starting goliath-%%i...
    start "goliath-%%i" cmd /c "cd /d "%DEV_DIR%\goliath-%%i" && java %JAVA_OPTS% -jar server.jar --nogui"
)

:: Wait for backends to start
echo Waiting 15s for Goliath instances to boot...
timeout /t 15 /nobreak >nul

:: Start Velocity
echo Starting Velocity proxy (port 25565)...
start "velocity" cmd /c "cd /d "%DEV_DIR%\velocity" && java -Xms256M -Xmx512M -jar velocity.jar"

echo.
echo === All servers started! ===
echo Connect to: localhost:25565
echo.
echo Each server runs in its own window.
echo Close the windows to stop them, or use stop-dev.bat
pause
