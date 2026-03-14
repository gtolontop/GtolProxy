@echo off
:: GtolProxy Dev Environment Setup
:: Downloads Velocity + Paper and sets up 3 Goliath instances

echo === GtolProxy Dev Setup ===

set DEV_DIR=%~dp0..\dev
set VELOCITY_DIR=%DEV_DIR%\velocity
set CURL=curl -L -o

:: Download Velocity
if not exist "%VELOCITY_DIR%\velocity.jar" (
    echo [1/3] Downloading Velocity...
    mkdir "%VELOCITY_DIR%\plugins" 2>nul
    %CURL% "%VELOCITY_DIR%\velocity.jar" "https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/449/downloads/velocity-3.4.0-SNAPSHOT-449.jar"
    echo Velocity downloaded.
) else (
    echo [1/3] Velocity already present, skipping.
)

:: Download Paper for each Goliath instance
echo [2/3] Setting up Goliath instances...

call :setup_goliath goliath-1 25566
call :setup_goliath goliath-2 25567
call :setup_goliath goliath-3 25568

:: Generate forwarding secret
echo [3/3] Creating forwarding secret...
set SECRET=gtol-dev-secret-%RANDOM%%RANDOM%
echo %SECRET%> "%VELOCITY_DIR%\forwarding.secret"

for %%i in (1 2 3) do (
    mkdir "%DEV_DIR%\goliath-%%i\config" 2>nul
    (
        echo proxies:
        echo   velocity:
        echo     enabled: true
        echo     online-mode: false
        echo     secret: "%SECRET%"
    ) > "%DEV_DIR%\goliath-%%i\config\paper-global.yml"
)

echo.
echo === Setup Complete ===
echo Next steps:
echo   1. Build with: scripts\build-and-deploy.bat
echo   2. Start with: scripts\start-dev.bat
goto :eof

:setup_goliath
set NAME=%1
set PORT=%2
set DIR=%DEV_DIR%\%NAME%

if not exist "%DIR%\server.jar" (
    echo Setting up %NAME% on port %PORT%...
    mkdir "%DIR%\plugins" 2>nul
    %CURL% "%DIR%\server.jar" "https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/194/downloads/paper-1.21.4-194.jar"
    echo eula=true> "%DIR%\eula.txt"
    (
        echo server-port=%PORT%
        echo online-mode=false
        echo view-distance=10
        echo simulation-distance=7
        echo max-players=100
        echo motd=%NAME% - Goliath Region
        echo level-name=world
        echo spawn-protection=0
    ) > "%DIR%\server.properties"
    echo %NAME% setup complete.
) else (
    echo %NAME% already set up, skipping.
)
goto :eof
