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
echo "🚀 Starting LingoLearn Application..."
chmod +x mvnw
./mvnw spring-boot:run
