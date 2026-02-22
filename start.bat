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
echo 🚀 Starting LingoLearn Application...
./mvnw spring-boot:run
pause
