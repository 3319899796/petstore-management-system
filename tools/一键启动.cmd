@echo off
chcp 65001 >nul
title PetStore Launcher
echo ================================================
echo  Pet Store System - starting up
echo ================================================
echo.
echo [1/3] Starting MySQL (wait 6s) ...
start "PetStore MySQL" "%~dp0start-mysql.cmd"
timeout /t 6 >nul

echo [2/3] Starting backend (wait 12s) ...
start "PetStore Backend" "%~dp0start-backend.cmd"
timeout /t 12 >nul

echo [3/3] Starting frontend and opening browser ...
call "%~dp0start-frontend.cmd"

echo.
echo All three windows should be running now.
echo   MySQL    : 3306
echo   Backend  : http://127.0.0.1:8080/api
echo   Frontend : http://127.0.0.1:5173
echo.
echo Accounts: admin / staff / owner   password: 123456
timeout /t 8 >nul
