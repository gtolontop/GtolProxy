package com.gtol.proxy.command;

import com.gtol.proxy.GtolProxy;
import com.gtol.proxy.model.GoliathRegion;
import com.gtol.proxy.model.PlayerState;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Admin command for managing Goliath Regions.
 * Usage:
 *   /gtol status           - Show all regions and player counts
 *   /gtol player <name>    - Show player's current region and position
 *   /gtol reload           - Reload regions config
 */
public class GtolCommand implements SimpleCommand {

    private final GtolProxy plugin;

    public GtolCommand(GtolProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> showStatus(invocation);
            case "player" -> showPlayer(invocation, args);
            case "reload" -> reloadConfig(invocation);
            default -> sendHelp(invocation);
        }
    }

    private void showStatus(Invocation invocation) {
        var source = invocation.source();
        var regionManager = plugin.getRegionManager();

        source.sendMessage(Component.text("=== Goliath Regions Status ===", NamedTextColor.GOLD));
        source.sendMessage(Component.text("Total regions: " + regionManager.getRegionCount(), NamedTextColor.YELLOW));
        source.sendMessage(Component.text("Online players tracked: " + regionManager.getAllPlayerStates().size(), NamedTextColor.YELLOW));
        source.sendMessage(Component.empty());

        for (GoliathRegion region : regionManager.getRegions()) {
            long playersInRegion = regionManager.getAllPlayerStates().stream()
                .filter(s -> region.getId().equals(s.getCurrentRegionId()))
                .count();

            String status = String.format("  %s [%s] - Chunks[%d,%d -> %d,%d] - %d players%s",
                region.getId(), region.getServerName(),
                region.getMinChunkX(), region.getMinChunkZ(),
                region.getMaxChunkX(), region.getMaxChunkZ(),
                playersInRegion,
                region.isDefault() ? " (DEFAULT)" : "");

            source.sendMessage(Component.text(status, NamedTextColor.GREEN));
        }
    }

    private void showPlayer(Invocation invocation, String[] args) {
        var source = invocation.source();
        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /gtol player <name>", NamedTextColor.RED));
            return;
        }

        String playerName = args[1];
        var playerOpt = plugin.getServer().getPlayer(playerName);
        if (playerOpt.isEmpty()) {
            source.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return;
        }

        var player = playerOpt.get();
        var stateOpt = plugin.getRegionManager().getPlayerState(player.getUniqueId());
        if (stateOpt.isEmpty()) {
            source.sendMessage(Component.text("No state tracked for: " + playerName, NamedTextColor.RED));
            return;
        }

        PlayerState state = stateOpt.get();
        source.sendMessage(Component.text("=== Player: " + playerName + " ===", NamedTextColor.GOLD));
        source.sendMessage(Component.text("Region: " + state.getCurrentRegionId(), NamedTextColor.YELLOW));
        source.sendMessage(Component.text(String.format("Position: %.1f, %.1f, %.1f", state.getX(), state.getY(), state.getZ()), NamedTextColor.YELLOW));
        source.sendMessage(Component.text("Transferring: " + state.isTransferring(), NamedTextColor.YELLOW));
    }

    private void reloadConfig(Invocation invocation) {
        plugin.getConfig().load();
        plugin.getRegionManager().loadRegions();
        invocation.source().sendMessage(
            Component.text("GtolProxy config reloaded! " + plugin.getRegionManager().getRegionCount() + " regions loaded.", NamedTextColor.GREEN));
    }

    private void sendHelp(Invocation invocation) {
        var source = invocation.source();
        source.sendMessage(Component.text("=== GtolProxy Commands ===", NamedTextColor.GOLD));
        source.sendMessage(Component.text("/gtol status - Show regions & players", NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/gtol player <name> - Player info", NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/gtol reload - Reload config", NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("gtol.admin");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return List.of("status", "player", "reload");
        }
        return List.of();
    }
}
