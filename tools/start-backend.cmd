@echo off
chcp 65001 >nul
title PetStore Backend (close this window to stop backend)
set MYSQL_PASSWORD=123456
echo Starting Spring Boot on http://127.0.0.1:8080/api ...
echo.
"C:\Program Files\Java\jdk-17\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%~dp0..\src\backend\target\pet-store-1.0.0.jar"
echo.
echo Backend stopped (exit code %ERRORLEVEL%).
pause
