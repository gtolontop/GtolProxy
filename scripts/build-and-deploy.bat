@echo off
:: Build both plugins and deploy to dev environment

echo === Building GtolProxy ===
cd /d "%~dp0.."
call mvn package -q
if errorlevel 1 (
    echo BUILD FAILED for GtolProxy!
    pause
    exit /b 1
)
echo GtolProxy built.

echo === Building GtolBackend ===
cd /d "%~dp0..\gtol-backend"
call mvn package -q
if errorlevel 1 (
    echo BUILD FAILED for GtolBackend!
    pause
    exit /b 1
)
echo GtolBackend built.

echo === Deploying to dev ===
set DEV_DIR=%~dp0..\dev

copy /Y "%~dp0..\target\gtol-proxy-1.0.0-SNAPSHOT.jar" "%DEV_DIR%\velocity\plugins\" >nul
echo Deployed gtol-proxy to velocity\plugins\

for %%i in (1 2 3) do (
    copy /Y "%~dp0..\gtol-backend\target\gtol-backend-1.0.0-SNAPSHOT.jar" "%DEV_DIR%\goliath-%%i\plugins\" >nul
    echo Deployed gtol-backend to goliath-%%i\plugins\
)

echo.
echo === Build ^& Deploy Complete ===
echo Restart servers to load new plugins.
pause
