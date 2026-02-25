#!/bin/bash

# Change directory to the script's location
cd "$(dirname "$0")"

echo "=========================================="
echo "✨ LingoLearn AI - Mac Auto-Launcher ✨"
echo "=========================================="

echo ""
echo "📥 Checking for updates from GitHub..."
git stash
git pull origin main
git stash pop

echo ""
echo "🚀 Handing over to run.sh..."
chmod +x run.sh
./run.sh
