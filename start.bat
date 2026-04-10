@echo off
setlocal enabledelayedexpansion
:: Set character code page to UTF-8 for 🌟 emojis
chcp 65001 >nul 2>&1

:: ============================================================
::  LingoLearn AI  –  Windows One-Click Launcher
::  Double-click this file to start the application.
::  No questions asked. Everything is handled automatically.
:: ============================================================

:: ── Always run from the folder that contains this script ────
cd /d "%~dp0"

:: ── Pretty banner ────────────────────────────────────────────
cls
echo.
echo   ╔══════════════════════════════════════════╗
echo   ║   🌟  LingoLearn AI  –  Starting Up  🌟   ║
echo   ╚══════════════════════════════════════════╝
echo.

:: ── 1. Check Java 17+ ────────────────────────────────────────
echo   ☕  Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo   ❌  Java is not installed.
    echo   👉  Download Java 17 from:  https://adoptium.net
    echo   📌  Install it, then double-click this file again.
    echo.
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JVER=%%v"
)
set "JVER=!JVER:"=!"
for /f "delims=." %%m in ("!JVER!") do set "JMAJOR=%%m"

if !JMAJOR! LSS 17 (
    echo.
    echo   ❌  Java 17 or newer is required (you have Java !JMAJOR!).
    echo   👉  Download Java 17 from:  https://adoptium.net
    echo.
    pause
    exit /b 1
)
echo   ✅  Java !JMAJOR! found.

:: ── 2. Pull latest code from GitHub ──────────────────────────
echo.
echo   📥  Pulling latest updates from GitHub...
git rev-parse --is-inside-work-tree >nul 2>&1
if not errorlevel 1 (
    git stash --quiet >nul 2>&1
    git pull origin main --quiet 2>&1
    git stash pop --quiet >nul 2>&1
    echo   ✅  Code is up to date.
) else (
    echo   ⚠️   Not a git repository – skipping update.
)

:: ── 3. Free up port 8080 if already in use ───────────────────
echo.
echo   🔍  Checking port 8080...
set "PORT_IN_USE=0"
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr /r ":8080 "') do (
    if not "%%a"=="0" (
        if "!PORT_IN_USE!"=="0" (
            set "PORT_IN_USE=1"
            echo   ⚙️   Port 8080 busy (PID %%a) – freeing it...
            taskkill /F /PID %%a >nul 2>&1
            timeout /t 1 >nul
        )
    )
)
if "!PORT_IN_USE!"=="0" (
    echo   ✅  Port 8080 is available.
) else (
    echo   ✅  Port 8080 is now free.
)

:: ── 4. Launch the application ────────────────────────────────
echo.
echo   🚀  Launching LingoLearn AI...
echo   ──────────────────────────────────────────────
echo.

call mvnw.cmd spring-boot:run --quiet -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
set EXIT_CODE=%errorlevel%

echo.
if %EXIT_CODE%==0 (
    echo   👋  LingoLearn has shut down cleanly.
) else (
    echo   ❌  LingoLearn stopped with error code %EXIT_CODE%.
    echo   📋  Scroll up to see what went wrong.
)
echo.
pause
endlocal
