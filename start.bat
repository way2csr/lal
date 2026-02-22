@echo off
echo ==========================================
echo ✨ LingoLearn AI - Windows Auto-Launcher ✨
echo ==========================================

echo.
echo 📥 Checking for updates from GitHub...
git stash
git pull origin main
git stash pop

echo.
echo 🔍 Checking port 8080...
FOR /F "tokens=5" %%a IN ('netstat -aon ^| findstr :8080') DO (
    IF NOT "%%a"=="0" (
        echo ⚙️  Process %%a is using port 8080. Killing it...
        taskkill /F /PID %%a >nul 2>&1
        timeout /t 1 >nul
    )
)

echo.
echo 🚀 Starting LingoLearn Application...
./mvnw spring-boot:run
pause
