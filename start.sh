#!/bin/bash

echo "=========================================="
echo "✨ LingoLearn AI - Mac/Linux Auto-Launcher ✨"
echo "=========================================="

echo ""
echo "📥 Checking for updates from GitHub..."
git stash
git pull origin main
git stash pop

echo ""
echo "� Checking port 8080..."
PID=$(lsof -ti :8080)
if [ ! -z "$PID" ]; then
    echo "⚙️  Process $PID is using port 8080. Killing it..."
    kill -9 $PID
    sleep 1
fi

echo ""
echo "�🚀 Starting LingoLearn Application..."
chmod +x mvnw
./mvnw spring-boot:run
