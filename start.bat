@echo off
setlocal enabledelayedexpansion

:: ============================================================
::  LingoLearn AI - Windows one-click launcher
::  Double-click this file to start the application.
:: ============================================================

cd /d "%~dp0"

cls
echo.
echo ========================================
echo   LingoLearn AI - Starting
echo ========================================
echo.

:: --- Check Java 17+ ---
echo [INFO] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] Java is not installed.
    echo [INFO] Download Java 17 from: https://adoptium.net
    echo [INFO] Install it, then double-click this file again.
    echo.
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JVER=%%~v"
)
for /f "delims=." %%m in ("!JVER!") do set "JMAJOR=%%m"

if !JMAJOR! LSS 17 (
    echo.
    echo [ERROR] Java 17 or newer is required. You have Java !JMAJOR!.
    echo [INFO] Download Java 17 from: https://adoptium.net
    echo.
    exit /b 1
)
echo [OK] Java !JMAJOR! found.

:: --- Pull latest from GitHub ---
echo.
echo [INFO] Pulling latest updates from GitHub...
set "GIT_TERMINAL_PROMPT=0"
git rev-parse --is-inside-work-tree >nul 2>&1
if not errorlevel 1 (
    git stash --quiet >nul 2>&1
    git pull origin main --quiet >nul 2>&1
    git stash pop --quiet >nul 2>&1
    echo [OK] Code is up to date.
) else (
    echo [WARN] Not a git repository - skipping update.
)

:: --- Free port 8080 if in use ---
echo.
echo [INFO] Checking port 8080...
set "PORT_IN_USE=0"
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr /r ":8080 "') do (
    if not "%%a"=="0" (
        if "!PORT_IN_USE!"=="0" (
            set "PORT_IN_USE=1"
            echo [INFO] Port 8080 busy PID %%a - freeing it...
            taskkill /F /PID %%a >nul 2>&1
            timeout /t 1 >nul
        )
    )
)
if "!PORT_IN_USE!"=="0" (
    echo [OK] Port 8080 is available.
) else (
    echo [OK] Port 8080 is now free.
)

:: --- Launch application ---
echo.
echo [INFO] Launching LingoLearn AI...
echo ----------------------------------------
echo.

call mvnw.cmd spring-boot:run --quiet -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
set EXIT_CODE=%errorlevel%

echo.
if %EXIT_CODE%==0 (
    echo [INFO] LingoLearn has shut down cleanly.
) else (
    echo [ERROR] LingoLearn stopped with error code %EXIT_CODE%.
    echo [INFO] Scroll up to see what went wrong.
)
echo.
endlocal
