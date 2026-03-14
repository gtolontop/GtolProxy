#!/bin/bash
# Stop all dev servers

DEV_DIR="$(cd "$(dirname "$0")/../dev" && pwd)"

echo "=== Stopping GtolProxy Dev Environment ==="

for i in 1 2 3; do
    PID_FILE="$DEV_DIR/goliath-$i/server.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        echo "Stopping goliath-$i (PID: $PID)..."
        kill "$PID" 2>/dev/null || true
        rm "$PID_FILE"
    fi
done

PID_FILE="$DEV_DIR/velocity/velocity.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    echo "Stopping Velocity (PID: $PID)..."
    kill "$PID" 2>/dev/null || true
    rm "$PID_FILE"
fi

echo "All servers stopped."
