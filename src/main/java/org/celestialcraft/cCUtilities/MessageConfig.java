package org.celestialcraft.cCUtilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class MessageConfig {

    private static YamlConfiguration config;
    private static File file; // cache the file so we can reload without recomputing
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void load(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /** Reload messages.yml. Safe to call from a /reload command. */
    public static void reload(JavaPlugin plugin) {
        // If plugin is provided, refresh the file handle and ensure default exists
        if (plugin != null) {
            file = new File(plugin.getDataFolder(), "messages.yml");
            if (!file.exists()) {
                plugin.saveResource("messages.yml", false);
            }
        }
        // If load() hasn't been called yet and no plugin provided, we can’t resolve the file
        if (file == null) {
            throw new IllegalStateException("MessageConfig not loaded yet. Call load(plugin) first or pass plugin to reload(plugin).");
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /** Convenience: reload using the cached file (after an initial load). */
    public static void reload() {
        if (file == null) {
            throw new IllegalStateException("MessageConfig not loaded yet. Call load(plugin) first or use reload(plugin).");
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static String get(String key) {
        String value = config.getString(key);
        if (value != null) return value;

        if (!key.startsWith("ced.") && config.contains("ced." + key)) {
            return config.getString("ced." + key);
        }

        if (!key.startsWith("customenderdragon.") && config.contains("customenderdragon." + key)) {
            return config.getString("customenderdragon." + key);
        }

        return "<red>Missing message: " + key;
    }

    public static boolean has(String key) {
        return config.contains(key)
                || config.contains("ced." + key)
                || config.contains("customenderdragon." + key);
    }

    public static Component mm(String key) {
        return miniMessage.deserialize(get(key));
    }

    public static List<String> getStringList(String path) {
        return config.getStringList(path);
    }
}
