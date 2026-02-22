#!/bin/bash
cd "$(dirname "$0")"

echo "=========================================="
echo "✨ LingoLearn AI - Starting Silently... ✨"
echo "=========================================="
echo "You can close this terminal window now."
echo ""

# Run the startup script in the background, detach it, and hide output
nohup ./start.sh > /dev/null 2>&1 &

# Wait for Spring Boot to initialize
sleep 8

# Open the default web browser to the application
open "http://localhost:8080/learn.html"
