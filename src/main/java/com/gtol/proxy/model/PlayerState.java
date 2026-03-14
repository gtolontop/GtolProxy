package com.gtol.proxy.model;

import java.util.UUID;

/**
 * Tracks a player's position and current region assignment.
 * Position is reported by the backend Folia instances via plugin messaging.
 */
public class PlayerState {

    private final UUID uuid;
    private final String name;
    private String currentRegionId;
    private double x, y, z;
    private float yaw, pitch;
    private boolean transferring;  // true while mid-transfer between regions
    private long lastPositionUpdate;

    public PlayerState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.transferring = false;
    }

    public void updatePosition(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lastPositionUpdate = System.currentTimeMillis();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public String getCurrentRegionId() { return currentRegionId; }
    public void setCurrentRegionId(String regionId) { this.currentRegionId = regionId; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public boolean isTransferring() { return transferring; }
    public void setTransferring(boolean transferring) { this.transferring = transferring; }

    public long getLastPositionUpdate() { return lastPositionUpdate; }
}
