package com.gtol.proxy.region;

import com.gtol.proxy.config.GtolConfig;
import com.gtol.proxy.model.GoliathRegion;
import com.gtol.proxy.model.PlayerState;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core of the Goliath system. Manages regions and handles player transfers
 * between server instances based on their world position.
 */
public class RegionManager {

    private final ProxyServer proxy;
    private final GtolConfig config;
    private final Logger logger;

    private final List<GoliathRegion> regions = new ArrayList<>();
    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private GoliathRegion defaultRegion;

    public RegionManager(ProxyServer proxy, GtolConfig config, Logger logger) {
        this.proxy = proxy;
        this.config = config;
        this.logger = logger;
    }

    public void loadRegions() {
        regions.clear();
        regions.addAll(config.getRegions());
        defaultRegion = regions.stream()
            .filter(GoliathRegion::isDefault)
            .findFirst()
            .orElse(regions.isEmpty() ? null : regions.get(0));

        if (defaultRegion == null) {
            logger.warn("No default region configured!");
        }
    }

    /**
     * Find which region contains the given block coordinates.
     */
    public Optional<GoliathRegion> getRegionAt(int blockX, int blockZ) {
        for (GoliathRegion region : regions) {
            if (region.containsBlock(blockX, blockZ)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    /**
     * Called when a backend server reports a player's position.
     * Checks if the player needs to be transferred to a different region.
     */
    public void handlePositionUpdate(UUID playerUuid, String playerName,
                                     double x, double y, double z, float yaw, float pitch) {
        PlayerState state = playerStates.computeIfAbsent(playerUuid,
            uuid -> new PlayerState(uuid, playerName));
        state.updatePosition(x, y, z, yaw, pitch);

        // Don't process if already mid-transfer
        if (state.isTransferring()) {
            return;
        }

        // Find which region the player is now in
        Optional<GoliathRegion> targetRegion = getRegionAt((int) x, (int) z);
        if (targetRegion.isEmpty()) {
            // Player is outside all defined regions - keep on current server
            return;
        }

        GoliathRegion target = targetRegion.get();
        String currentRegionId = state.getCurrentRegionId();

        // If player is in a different region than their current server, transfer them
        if (currentRegionId != null && !currentRegionId.equals(target.getId())) {
            transferPlayer(playerUuid, state, target);
        }
    }

    /**
     * Seamlessly transfer a player to a different Goliath region/server.
     */
    private void transferPlayer(UUID playerUuid, PlayerState state, GoliathRegion targetRegion) {
        Optional<Player> playerOpt = proxy.getPlayer(playerUuid);
        if (playerOpt.isEmpty()) return;

        Player player = playerOpt.get();
        Optional<RegisteredServer> serverOpt = proxy.getServer(targetRegion.getServerName());
        if (serverOpt.isEmpty()) {
            logger.error("Target server '{}' for region '{}' not found in Velocity!",
                targetRegion.getServerName(), targetRegion.getId());
            return;
        }

        state.setTransferring(true);
        RegisteredServer targetServer = serverOpt.get();

        logger.info("Transferring player {} from region {} to region {} (server: {})",
            player.getUsername(), state.getCurrentRegionId(),
            targetRegion.getId(), targetRegion.getServerName());

        // Velocity handles the seamless server switch
        player.createConnectionRequest(targetServer).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                state.setCurrentRegionId(targetRegion.getId());
                logger.info("Player {} successfully transferred to region {}",
                    player.getUsername(), targetRegion.getId());
            } else {
                logger.error("Failed to transfer player {} to region {}: {}",
                    player.getUsername(), targetRegion.getId(), result.getReasonComponent());
            }
            state.setTransferring(false);
        }).exceptionally(ex -> {
            logger.error("Exception transferring player {}", player.getUsername(), ex);
            state.setTransferring(false);
            return null;
        });
    }

    /**
     * Register a player when they first connect.
     */
    public PlayerState registerPlayer(UUID uuid, String name) {
        PlayerState state = new PlayerState(uuid, name);
        if (defaultRegion != null) {
            state.setCurrentRegionId(defaultRegion.getId());
        }
        playerStates.put(uuid, state);
        return state;
    }

    /**
     * Remove player state on disconnect.
     */
    public void unregisterPlayer(UUID uuid) {
        playerStates.remove(uuid);
    }

    public Optional<PlayerState> getPlayerState(UUID uuid) {
        return Optional.ofNullable(playerStates.get(uuid));
    }

    public GoliathRegion getDefaultRegion() { return defaultRegion; }
    public int getRegionCount() { return regions.size(); }
    public List<GoliathRegion> getRegions() { return Collections.unmodifiableList(regions); }
    public Collection<PlayerState> getAllPlayerStates() { return playerStates.values(); }
}
