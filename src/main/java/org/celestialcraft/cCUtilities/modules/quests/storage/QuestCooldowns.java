package org.celestialcraft.cCUtilities.modules.quests.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestCooldowns {

    private static final Map<UUID, Long> weeklyTimestamps = new HashMap<>();
    private static File file;
    private static YamlConfiguration config;
    private static Plugin plugin;

    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        file = new File(plugin.getDataFolder(), "questscooldown.yml");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("Created questscooldown.yml for QuestCooldowns.");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create questscooldown.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public static void setLastWeeklyClaim(UUID uuid, long timeMillis) {
        weeklyTimestamps.put(uuid, timeMillis);
    }

    public static Long getLastWeeklyClaim(UUID uuid) {
        return weeklyTimestamps.get(uuid);
    }

    public static void save() {
        config.options().copyDefaults(true);

        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Map.Entry<UUID, Long> entry : weeklyTimestamps.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save questscooldown.yml: " + e.getMessage());
        }
    }

    private static void load() {
        if (!file.exists()) return;

        for (String key : config.getKeys(false)) {
            long time = config.getLong(key);
            weeklyTimestamps.put(UUID.fromString(key), time);
        }
    }
}
