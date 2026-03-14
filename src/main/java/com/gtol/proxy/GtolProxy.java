package com.gtol.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.gtol.proxy.config.GtolConfig;
import com.gtol.proxy.region.RegionManager;
import com.gtol.proxy.listener.PlayerRegionListener;
import com.gtol.proxy.command.GtolCommand;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "gtol-proxy",
    name = "GtolProxy",
    version = "1.0.0-SNAPSHOT",
    description = "Goliath Regions - Seamless region-based server distribution",
    authors = {"GtolTeam"}
)
public class GtolProxy {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private GtolConfig config;
    private RegionManager regionManager;

    @Inject
    public GtolProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("=== GtolProxy - Goliath Regions ===");
        logger.info("Initializing region system...");

        // Load config
        this.config = new GtolConfig(dataDirectory, logger);
        config.load();

        // Init region manager
        this.regionManager = new RegionManager(server, config, logger);
        regionManager.loadRegions();

        // Register listeners
        server.getEventManager().register(this, new PlayerRegionListener(this));

        // Register commands
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("gtol").build(),
            new GtolCommand(this)
        );

        logger.info("GtolProxy loaded! {} regions configured.", regionManager.getRegionCount());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("GtolProxy shutting down...");
    }

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public GtolConfig getConfig() { return config; }
    public RegionManager getRegionManager() { return regionManager; }
}
