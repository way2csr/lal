#!/bin/bash

echo ""
echo "🔍 Checking port 8080..."
PID=$(lsof -ti :8080)
if [ ! -z "$PID" ]; then
    echo "⚙️  Process $PID is using port 8080. Killing it..."
    kill -9 $PID
    sleep 1
fi

# Infisical Machine Identity credentials (kept here instead of hardcoded in Java)
export INFISICAL_CLIENT_ID="76ede7dc-d60a-44cf-8664-5650f460d49d"
export INFISICAL_CLIENT_SECRET="ae9fea28dd9c0f727706197019670707c3c00b069e52399738062f5b33b1b295"
export INFISICAL_PROJECT_ID="397abbdb-c673-4d6e-8d8e-ed0bc928f2d6"
export INFISICAL_ENV="dev"

echo ""
echo "🚀 Starting LingoLearn Application..."
chmod +x mvnw
./mvnw spring-boot:run
