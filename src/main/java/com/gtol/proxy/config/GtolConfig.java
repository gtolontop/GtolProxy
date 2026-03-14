package com.gtol.proxy.config;

import com.gtol.proxy.model.GoliathRegion;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Loads region configuration from regions.yml.
 * Simple key=value parser (no YAML lib dependency for now).
 */
public class GtolConfig {

    private final Path dataDirectory;
    private final Logger logger;

    private final List<GoliathRegion> regions = new ArrayList<>();
    private int borderZoneChunks = 4;       // chunks before edge to start pre-loading
    private int positionUpdateInterval = 5; // ticks between position reports from backend
    private String defaultRegionId = "goliath-1";

    public GtolConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void load() {
        try {
            Files.createDirectories(dataDirectory);
            Path configFile = dataDirectory.resolve("regions.conf");

            if (!Files.exists(configFile)) {
                createDefault(configFile);
            }

            parseConfig(configFile);
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }
    }

    private void createDefault(Path configFile) throws IOException {
        String defaultConfig = """
                # GtolProxy - Goliath Regions Configuration
                # Each region maps a chunk area to a backend server instance.
                #
                # Format:
                # region.<id>.server = <velocity server name>
                # region.<id>.minChunkX = <int>
                # region.<id>.maxChunkX = <int>
                # region.<id>.minChunkZ = <int>
                # region.<id>.maxChunkZ = <int>
                # region.<id>.default = true/false

                # Global settings
                border-zone-chunks = 4
                position-update-interval = 5
                default-region = goliath-1

                # Region 1: Spawn area (center of the world)
                region.goliath-1.server = goliath-1
                region.goliath-1.minChunkX = -62
                region.goliath-1.maxChunkX = 62
                region.goliath-1.minChunkZ = -62
                region.goliath-1.maxChunkZ = 62
                region.goliath-1.default = true

                # Region 2: North (negative Z)
                region.goliath-2.server = goliath-2
                region.goliath-2.minChunkX = -62
                region.goliath-2.maxChunkX = 62
                region.goliath-2.minChunkZ = -187
                region.goliath-2.maxChunkZ = -63
                region.goliath-2.default = false

                # Region 3: South (positive Z)
                region.goliath-3.server = goliath-3
                region.goliath-3.minChunkX = -62
                region.goliath-3.maxChunkX = 62
                region.goliath-3.minChunkZ = 63
                region.goliath-3.maxChunkZ = 187
                region.goliath-3.default = false
                """;

        Files.writeString(configFile, defaultConfig);
        logger.info("Created default regions.conf");
    }

    private void parseConfig(Path configFile) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            props.load(reader);
        }

        // Global settings
        borderZoneChunks = Integer.parseInt(props.getProperty("border-zone-chunks", "4"));
        positionUpdateInterval = Integer.parseInt(props.getProperty("position-update-interval", "5"));
        defaultRegionId = props.getProperty("default-region", "goliath-1");

        // Parse regions
        Set<String> regionIds = new LinkedHashSet<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("region.")) {
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    regionIds.add(parts[1]);
                }
            }
        }

        regions.clear();
        for (String id : regionIds) {
            String prefix = "region." + id + ".";
            String server = props.getProperty(prefix + "server", id);
            int minCX = Integer.parseInt(props.getProperty(prefix + "minChunkX", "0"));
            int maxCX = Integer.parseInt(props.getProperty(prefix + "maxChunkX", "0"));
            int minCZ = Integer.parseInt(props.getProperty(prefix + "minChunkZ", "0"));
            int maxCZ = Integer.parseInt(props.getProperty(prefix + "maxChunkZ", "0"));
            boolean isDefault = Boolean.parseBoolean(props.getProperty(prefix + "default", "false"));

            GoliathRegion region = new GoliathRegion(id, server, minCX, maxCX, minCZ, maxCZ, isDefault);
            regions.add(region);
            logger.info("Loaded region: {}", region);
        }
    }

    public List<GoliathRegion> getRegions() { return Collections.unmodifiableList(regions); }
    public int getBorderZoneChunks() { return borderZoneChunks; }
    public int getPositionUpdateInterval() { return positionUpdateInterval; }
    public String getDefaultRegionId() { return defaultRegionId; }
}
