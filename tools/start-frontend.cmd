@echo off
chcp 65001 >nul
title PetStore Frontend
set NODE_EXE=C:\Users\33198\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\node\bin\node.exe

if not exist "%NODE_EXE%" (
  echo [ERROR] Node.js not found.
  echo Looked for: %NODE_EXE%
  echo Install Node.js 18+ and edit NODE_EXE in this file.
  pause
  exit /b 1
)

if not exist "%~dp0..\src\frontend\dist\index.html" (
  echo [ERROR] Frontend build not found: src\frontend\dist\index.html
  echo Run "npm run build" in src\frontend first.
  pause
  exit /b 1
)

echo Starting frontend server on http://127.0.0.1:5173 ...
start "PetStore Frontend Server" "%NODE_EXE%" "%~dp0serve-frontend.mjs"
timeout /t 3 >nul
start "" http://127.0.0.1:5173
echo Opened http://127.0.0.1:5173 in your browser.
echo Keep the "PetStore Frontend Server" window open; closing it stops the site.
timeout /t 5 >nul
