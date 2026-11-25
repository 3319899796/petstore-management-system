@echo off
chcp 65001 >nul
title PetStore MySQL 8.0 (close this window to stop database)
echo Starting MySQL on 127.0.0.1:3306 ...
echo Data dir: D:\petstore-rt\mysql-data
echo.
"D:\petstore-rt\tools\mysql-8.0.27-winx64\bin\mysqld.exe" --defaults-file=D:\petstore-rt\tools\my.ini --console
echo.
echo MySQL stopped (exit code %ERRORLEVEL%).
pause
