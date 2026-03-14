package com.gtol.proxy.model;

/**
 * Represents a geographic region of the world assigned to a specific server instance.
 * Regions are defined by chunk coordinate boundaries.
 */
public class GoliathRegion {

    private final String id;          // e.g. "goliath-1"
    private final String serverName;  // Velocity registered server name
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private final boolean isDefault;  // spawn region

    public GoliathRegion(String id, String serverName, int minChunkX, int maxChunkX,
                         int minChunkZ, int maxChunkZ, boolean isDefault) {
        this.id = id;
        this.serverName = serverName;
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkZ = maxChunkZ;
        this.isDefault = isDefault;
    }

    /**
     * Check if a block position falls within this region.
     */
    public boolean containsBlock(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return containsChunk(chunkX, chunkZ);
    }

    /**
     * Check if a chunk coordinate falls within this region.
     */
    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX
            && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    /**
     * Check if a block position is within the border zone (close to edge).
     * Used to pre-load chunks on the neighboring instance.
     */
    public boolean isInBorderZone(int blockX, int blockZ, int borderChunks) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return containsChunk(chunkX, chunkZ) && (
            chunkX - minChunkX < borderChunks ||
            maxChunkX - chunkX < borderChunks ||
            chunkZ - minChunkZ < borderChunks ||
            maxChunkZ - chunkZ < borderChunks
        );
    }

    public String getId() { return id; }
    public String getServerName() { return serverName; }
    public int getMinChunkX() { return minChunkX; }
    public int getMaxChunkX() { return maxChunkX; }
    public int getMinChunkZ() { return minChunkZ; }
    public int getMaxChunkZ() { return maxChunkZ; }
    public boolean isDefault() { return isDefault; }

    @Override
    public String toString() {
        return String.format("GoliathRegion{id='%s', server='%s', chunks=[%d,%d]->[%d,%d]}",
            id, serverName, minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }
}
