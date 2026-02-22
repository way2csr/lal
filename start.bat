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
echo � Handing over to run.bat...
call run.bat
