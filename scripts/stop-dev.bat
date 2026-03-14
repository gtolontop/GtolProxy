@echo off
:: Stop all dev servers by killing java processes with their window titles

echo === Stopping GtolProxy Dev Environment ===

taskkill /FI "WINDOWTITLE eq goliath-1*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq goliath-2*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq goliath-3*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq velocity*" /T /F >nul 2>&1

echo All servers stopped.
pause
