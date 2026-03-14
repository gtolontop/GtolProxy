#!/bin/bash
# Build both plugins and deploy to dev environment

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEV_DIR="$ROOT_DIR/dev"

echo "=== Building GtolProxy ==="
cd "$ROOT_DIR"
mvn package -q
echo "GtolProxy built."

echo "=== Building GtolBackend ==="
cd "$ROOT_DIR/gtol-backend"
mvn package -q
echo "GtolBackend built."

echo "=== Deploying to dev ==="

# Copy proxy plugin to Velocity
cp "$ROOT_DIR/target/gtol-proxy-1.0.0-SNAPSHOT.jar" "$DEV_DIR/velocity/plugins/"
echo "Deployed gtol-proxy to velocity/plugins/"

# Copy backend plugin to all Goliath instances
for i in 1 2 3; do
    cp "$ROOT_DIR/gtol-backend/target/gtol-backend-1.0.0-SNAPSHOT.jar" "$DEV_DIR/goliath-$i/plugins/"
    echo "Deployed gtol-backend to goliath-$i/plugins/"
done

echo ""
echo "=== Build & Deploy Complete ==="
echo "Restart servers to load new plugins."
