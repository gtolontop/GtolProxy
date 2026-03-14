#!/bin/bash
# Start all dev servers: 3 Goliath instances + Velocity proxy
# Run this from the GtolProxy root directory

set -e

DEV_DIR="$(cd "$(dirname "$0")/../dev" && pwd)"
JAVA_CMD="java"
JAVA_OPTS="-Xms512M -Xmx1G"

echo "=== Starting GtolProxy Dev Environment ==="

# Start Goliath instances in background
for i in 1 2 3; do
    GOLIATH_DIR="$DEV_DIR/goliath-$i"
    if [ ! -f "$GOLIATH_DIR/server.jar" ]; then
        echo "ERROR: goliath-$i not set up. Run setup-dev.sh first!"
        exit 1
    fi

    echo "Starting goliath-$i (port 2556$((5+i)))..."
    cd "$GOLIATH_DIR"
    $JAVA_CMD $JAVA_OPTS -jar server.jar --nogui &
    echo $! > "$GOLIATH_DIR/server.pid"
    cd - > /dev/null
done

# Wait a bit for backends to start
echo "Waiting 10s for Goliath instances to start..."
sleep 10

# Start Velocity
VELOCITY_DIR="$DEV_DIR/velocity"
echo "Starting Velocity proxy (port 25565)..."
cd "$VELOCITY_DIR"
$JAVA_CMD -Xms256M -Xmx512M -jar velocity.jar &
echo $! > "$VELOCITY_DIR/velocity.pid"
cd - > /dev/null

echo ""
echo "=== All servers started! ==="
echo "Connect to: localhost:25565"
echo ""
echo "To stop all: ./scripts/stop-dev.sh"
