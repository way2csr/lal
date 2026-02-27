#!/bin/bash

echo "=========================================="
echo "✨ LingoLearn AI - Mac/Linux Auto-Launcher ✨"
echo "=========================================="

echo ""
echo "📥 Checking for updates from GitHub..."
git stash >/dev/null 2>&1
git pull origin main
git stash pop >/dev/null 2>&1

echo ""
echo "🔐 Checking Infisical Credentials..."
LOCAL_YAML="src/main/resources/application-local.yaml"
HAS_CREDENTIALS=0

if [ -f "$LOCAL_YAML" ]; then
    if grep -q "client-id" "$LOCAL_YAML" && grep -q "client-secret" "$LOCAL_YAML"; then
        HAS_CREDENTIALS=1
        echo "✅ Found credentials in application-local.yaml"
    fi
fi

if [ $HAS_CREDENTIALS -eq 0 ]; then
    if [ ! -z "$INFISICAL_CLIENT_ID" ] && [ ! -z "$INFISICAL_CLIENT_SECRET" ]; then
        HAS_CREDENTIALS=1
        echo "✅ Found credentials in environment variables"
    fi
fi

if [ $HAS_CREDENTIALS -eq 0 ]; then
    echo "⚠️  WARNING: Infisical credentials not found!"
    echo "   Secrets will not be loaded from the cloud."
    echo "   Please copy src/main/resources/application-local.yaml.sample"
    echo "   to src/main/resources/application-local.yaml and add your keys."
    echo ""
    read -p "Continue anyway? (y/n): " CONTINUE
    if [ "$CONTINUE" != "y" ]; then
        exit 1
    fi
fi

echo ""
echo "🚀 Handing over to run.sh..."
chmod +x run.sh
./run.sh
