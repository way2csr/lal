@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo ✨ LingoLearn AI - Windows Auto-Launcher ✨
echo ==========================================

echo.
echo 📥 Checking for updates from GitHub...
git stash >nul 2>&1
git pull origin main
git stash pop >nul 2>&1

echo.
echo 🔐 Checking Infisical Credentials...
set "LOCAL_YAML=src\main\resources\application-local.yaml"
set "HAS_CREDENTIALS=0"

if exist "!LOCAL_YAML!" (
    findstr "client-id" "!LOCAL_YAML!" >nul && findstr "client-secret" "!LOCAL_YAML!" >nul && (
        set "HAS_CREDENTIALS=1"
        echo ✅ Found credentials in application-local.yaml
    )
)

if "!HAS_CREDENTIALS!"=="0" (
    if not "!INFISICAL_CLIENT_ID!"=="" if not "!INFISICAL_CLIENT_SECRET!"=="" (
        set "HAS_CREDENTIALS=1"
        echo ✅ Found credentials in environment variables
    )
)

if "!HAS_CREDENTIALS!"=="0" (
    echo ⚠️  WARNING: Infisical credentials not found!
    echo    Secrets will not be loaded from the cloud.
    echo    Please copy src\main\resources\application-local.yaml.sample
    echo    to src\main\resources\application-local.yaml and add your keys.
    echo.
    set /p "CONTINUE=Continue anyway? (y/n): "
    if /i "!CONTINUE!" NEQ "y" exit /b
)

echo.
echo 🚀 Handing over to run.bat...
call run.bat
