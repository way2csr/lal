#!/bin/bash
# ============================================================
#  LingoLearn AI  –  macOS / Linux One-Click Launcher
#  Double-click this file (or run:  bash start.sh)
#  No questions asked. Everything is handled automatically.
# ============================================================

# ── Always run from the folder that contains this script ────
cd "$(dirname "$0")" || exit 1

# ── Pretty banner ────────────────────────────────────────────
clear
echo ""
echo "  ╔══════════════════════════════════════════╗"
echo "  ║   🌟  LingoLearn AI  –  Starting Up  🌟   ║"
echo "  ╚══════════════════════════════════════════╝"
echo ""

# ── 1. Check Java 17+ ────────────────────────────────────────
echo "  ☕  Checking Java..."
if ! command -v java &>/dev/null; then
    echo ""
    echo "  ❌  Java is not installed."
    echo "  👉  Download Java 17 from:  https://adoptium.net"
    echo "  📌  Install it, then double-click this file again."
    echo ""
    read -r -p "  Press ENTER to close..." _
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ -z "$JAVA_VER" ] || [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    echo ""
    echo "  ❌  Java 17 or newer is required (you have Java $JAVA_VER)."
    echo "  👉  Download Java 17 from:  https://adoptium.net"
    echo ""
    read -r -p "  Press ENTER to close..." _
    exit 1
fi
echo "  ✅  Java $JAVA_VER found."

# ── 2. Pull latest code from GitHub ──────────────────────────
echo ""
echo "  📥  Pulling latest updates from GitHub..."
if git rev-parse --is-inside-work-tree &>/dev/null; then
    git stash --quiet 2>/dev/null
    git pull origin main --quiet 2>&1 | sed 's/^/      /'
    git stash pop --quiet 2>/dev/null
    echo "  ✅  Code is up to date."
else
    echo "  ⚠️   Not a git repository – skipping update."
fi

# ── 3. Free up port 8080 if already in use ───────────────────
echo ""
echo "  🔍  Checking port 8080..."
PID=$(lsof -ti :8080 2>/dev/null)
if [ -n "$PID" ]; then
    echo "  ⚙️   Port 8080 busy (PID $PID) – freeing it..."
    kill -9 "$PID" 2>/dev/null
    sleep 1
    echo "  ✅  Port 8080 is now free."
else
    echo "  ✅  Port 8080 is available."
fi

# ── 4. Make Maven wrapper executable & launch ────────────────
echo ""
echo "  🚀  Launching LingoLearn AI..."
echo "  ──────────────────────────────────────────────"
echo ""
chmod +x mvnw
./mvnw spring-boot:run --quiet -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "  👋  LingoLearn has shut down cleanly."
else
    echo "  ❌  LingoLearn stopped with error code $EXIT_CODE."
    echo "  📋  Scroll up to see what went wrong."
    echo ""
    read -r -p "  Press ENTER to close..." _
fi
