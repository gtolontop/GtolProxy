#!/bin/bash
# GtolProxy Dev Environment Setup
# Downloads Velocity + Folia and sets up 3 Goliath instances

set -e

DEV_DIR="$(cd "$(dirname "$0")/../dev" && pwd)"
JAVA_CMD="java"

echo "=== GtolProxy Dev Setup ==="
echo "Dev directory: $DEV_DIR"

# Download Velocity
VELOCITY_DIR="$DEV_DIR/velocity"
if [ ! -f "$VELOCITY_DIR/velocity.jar" ]; then
    echo "[1/3] Downloading Velocity..."
    mkdir -p "$VELOCITY_DIR/plugins"
    curl -L -o "$VELOCITY_DIR/velocity.jar" \
        "https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/449/downloads/velocity-3.4.0-SNAPSHOT-449.jar"
    echo "Velocity downloaded."
else
    echo "[1/3] Velocity already present, skipping."
fi

# Download Folia for each Goliath instance
download_folia() {
    local name=$1
    local port=$2
    local dir="$DEV_DIR/$name"

    if [ ! -f "$dir/server.jar" ]; then
        echo "Downloading Folia for $name..."
        mkdir -p "$dir/plugins"
        # Using Paper for dev (Folia builds need to be compiled from source)
        # Replace with your GtolFolia build later
        curl -L -o "$dir/server.jar" \
            "https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/194/downloads/paper-1.21.4-194.jar"

        # Accept EULA
        echo "eula=true" > "$dir/eula.txt"

        # Server properties
        cat > "$dir/server.properties" << EOF
server-port=$port
online-mode=false
view-distance=10
simulation-distance=7
max-players=100
motd=$name - Goliath Region
level-name=world
spawn-protection=0
EOF
        echo "Setup $name on port $port"
    else
        echo "$name already set up, skipping."
    fi
}

echo "[2/3] Setting up Goliath instances..."
download_folia "goliath-1" 25566
download_folia "goliath-2" 25567
download_folia "goliath-3" 25568

echo "[3/3] Creating forwarding secret..."
# Generate a forwarding secret for Velocity modern forwarding
SECRET="gtol-dev-secret-$(date +%s)"
echo "$SECRET" > "$VELOCITY_DIR/forwarding.secret"
for i in 1 2 3; do
    mkdir -p "$DEV_DIR/goliath-$i/config"
    # Paper needs paper-global.yml for velocity forwarding
    cat > "$DEV_DIR/goliath-$i/config/paper-global.yml" << EOF
proxies:
  velocity:
    enabled: true
    online-mode: false
    secret: "$SECRET"
EOF
done

echo ""
echo "=== Setup Complete ==="
echo "Next steps:"
echo "  1. Build GtolProxy:  cd gtol-proxy && mvn package"
echo "  2. Build GtolBackend: cd gtol-backend && mvn package"
echo "  3. Copy gtol-proxy.jar to dev/velocity/plugins/"
echo "  4. Copy gtol-backend.jar to dev/goliath-{1,2,3}/plugins/"
echo "  5. Run: ./scripts/start-dev.sh"
