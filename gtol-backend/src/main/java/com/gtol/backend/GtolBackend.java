package com.gtol.backend;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Backend plugin that runs on each Folia/GtolFolia instance.
 * Periodically sends player positions to the Velocity proxy via plugin messaging.
 * The proxy uses this data to decide when to transfer players between regions.
 */
public class GtolBackend extends JavaPlugin {

    private static final String CHANNEL = "gtol:position";
    private int updateIntervalTicks = 5; // every 5 ticks = 250ms

    @Override
    public void onEnable() {
        getLogger().info("GtolBackend starting - Position reporter for Goliath Regions");

        // Register outgoing plugin channel
        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, CHANNEL);

        // Schedule position reporting task
        // Using Folia's global region scheduler for compatibility
        schedulePositionReporter();

        getLogger().info("GtolBackend enabled! Reporting positions every " + updateIntervalTicks + " ticks");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getLogger().info("GtolBackend disabled");
    }

    private void schedulePositionReporter() {
        // Use Folia's async scheduler to periodically send positions
        // This runs on Folia's global region scheduler
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendPositionUpdate(player);
            }
        }, updateIntervalTicks, updateIntervalTicks);
    }

    /**
     * Send a player's position to the proxy via plugin messaging.
     * Protocol: [UUID most] [UUID least] [x] [y] [z] [yaw] [pitch] [name]
     */
    private void sendPositionUpdate(Player player) {
        if (!player.isOnline()) return;

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);

            // UUID
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());

            // Position
            out.writeDouble(player.getLocation().getX());
            out.writeDouble(player.getLocation().getY());
            out.writeDouble(player.getLocation().getZ());
            out.writeFloat(player.getLocation().getYaw());
            out.writeFloat(player.getLocation().getPitch());

            // Name
            out.writeUTF(player.getName());

            out.flush();
            player.sendPluginMessage(this, CHANNEL, byteOut.toByteArray());
        } catch (IOException e) {
            getLogger().warning("Failed to send position for " + player.getName() + ": " + e.getMessage());
        }
    }
}
