package com.gtol.proxy.listener;

import com.gtol.proxy.GtolProxy;
import com.gtol.proxy.model.GoliathRegion;
import com.gtol.proxy.model.PlayerState;
import com.gtol.proxy.region.RegionManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Listens for player events and plugin messages to manage region transitions.
 *
 * The backend Folia servers send position updates via plugin messaging channel.
 * This listener processes those updates and triggers transfers when needed.
 */
public class PlayerRegionListener {

    public static final MinecraftChannelIdentifier GTOL_CHANNEL =
        MinecraftChannelIdentifier.create("gtol", "position");

    private final GtolProxy plugin;
    private final RegionManager regionManager;

    public PlayerRegionListener(GtolProxy plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();

        // Register the plugin messaging channel
        plugin.getServer().getChannelRegistrar().register(GTOL_CHANNEL);
    }

    /**
     * When a player first connects, send them to the default region.
     */
    @Subscribe
    public void onPlayerChooseServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        PlayerState state = regionManager.registerPlayer(player.getUniqueId(), player.getUsername());

        GoliathRegion defaultRegion = regionManager.getDefaultRegion();
        if (defaultRegion != null) {
            plugin.getServer().getServer(defaultRegion.getServerName())
                .ifPresent(event::setInitialServer);
            plugin.getLogger().info("Player {} joining default region {} (server: {})",
                player.getUsername(), defaultRegion.getId(), defaultRegion.getServerName());
        }
    }

    /**
     * Track when a player successfully connects to a backend server.
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();

        regionManager.getPlayerState(player.getUniqueId()).ifPresent(state -> {
            // Find which region this server belongs to
            for (GoliathRegion region : regionManager.getRegions()) {
                if (region.getServerName().equals(serverName)) {
                    state.setCurrentRegionId(region.getId());
                    break;
                }
            }
        });
    }

    /**
     * Clean up when a player disconnects.
     */
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        regionManager.unregisterPlayer(event.getPlayer().getUniqueId());
        plugin.getLogger().debug("Player {} disconnected, state cleaned up",
            event.getPlayer().getUsername());
    }

    /**
     * Receive position updates from backend Folia instances.
     *
     * Protocol: gtol:position channel
     * Data format: [UUID(16 bytes)] [double x] [double y] [double z] [float yaw] [float pitch]
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(GTOL_CHANNEL)) return;

        // Only process messages from backend servers
        if (!(event.getSource() instanceof ServerConnection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        byte[] data = event.getData();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            long uuidMost = in.readLong();
            long uuidLeast = in.readLong();
            java.util.UUID uuid = new java.util.UUID(uuidMost, uuidLeast);

            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            float yaw = in.readFloat();
            float pitch = in.readFloat();

            String playerName = "";
            if (in.available() > 0) {
                playerName = in.readUTF();
            }

            regionManager.handlePositionUpdate(uuid, playerName, x, y, z, yaw, pitch);

        } catch (IOException e) {
            plugin.getLogger().error("Failed to read position plugin message", e);
        }
    }
}
