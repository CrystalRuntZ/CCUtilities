package org.celestialcraft.cCUtilities.modules.entitymanager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityLimitManager {
    private static final Map<String, Integer> mobLimits = new HashMap<>();
    private static final Map<Material, Integer> blockLimits = new HashMap<>();
    private static int totalEntityLimit = 256;
    private static String blockLimitMessage = "<red>You cannot place any more <block> blocks in this chunk. Limit: <limit>";

    public static void load(FileConfiguration config) {
        mobLimits.clear();
        totalEntityLimit = config.getInt("entitymanager.total-entities", 256);

        ConfigurationSection mobSection = config.getConfigurationSection("entitymanager.mobs");
        if (mobSection != null) {
            for (String key : mobSection.getKeys(false)) {
                int limit = mobSection.getInt(key);
                mobLimits.put(key.toUpperCase(), limit);
            }
        }

        blockLimits.clear();
        ConfigurationSection blockSection = config.getConfigurationSection("entitymanager.blocks");
        if (blockSection != null) {
            for (String key : blockSection.getKeys(false)) {
                Material material = Material.matchMaterial(key.toUpperCase());
                int limit = blockSection.getInt(key);
                if (material != null) {
                    blockLimits.put(material, limit);
                }
            }
        }

        blockLimitMessage = config.getString("entitymanager.block-limit-message", blockLimitMessage);
    }

    public static Integer getMobLimit(String mob) {
        return mobLimits.get(mob.toUpperCase());
    }

    public static void setMobLimit(String mob, int limit) {
        mobLimits.put(mob.toUpperCase(), limit);
    }

    public static int getTotalLimit() {
        return totalEntityLimit;
    }

    public static void setBlockLimit(Material material, int limit) {
        blockLimits.put(material, limit);
    }

    public static Integer getBlockLimit(Material material) {
        return blockLimits.get(material);
    }

    public static String getBlockLimitMessage() {
        return MessageConfig.get("entitymanager.block-limit-message");
    }

    public static Map<String, Integer> getAllMobLimits() {
        return Collections.unmodifiableMap(mobLimits);
    }

    public static Map<Material, Integer> getAllBlockLimits() {
        return Collections.unmodifiableMap(blockLimits);
    }

    public static void saveAllLimitsToConfig(FileConfiguration config, JavaPlugin plugin) {
        ConfigurationSection mobSection = config.getConfigurationSection("entitymanager.mobs");
        if (mobSection == null) {
            mobSection = config.createSection("entitymanager.mobs");
        }
        for (Map.Entry<String, Integer> entry : mobLimits.entrySet()) {
            String mob = entry.getKey();
            int limit = entry.getValue();
            if (mobSection.getInt(mob) != limit) {
                mobSection.set(mob, limit);
            }
        }

        ConfigurationSection blockSection = config.getConfigurationSection("entitymanager.blocks");
        if (blockSection == null) {
            blockSection = config.createSection("entitymanager.blocks");
        }
        for (Map.Entry<Material, Integer> entry : blockLimits.entrySet()) {
            Material material = entry.getKey();
            int limit = entry.getValue();
            if (blockSection.getInt(material.name()) != limit) {
                blockSection.set(material.name(), limit);
            }
        }

        plugin.saveConfig();
    }
}
