package org.celestialcraft.cCUtilities.modules.modulemanager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.io.File;
import java.io.IOException;

public class ModulesConfig {

    private static final JavaPlugin plugin = CCUtilities.getInstance();
    private static File file;
    private static FileConfiguration config;

    public static void reload() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "modulemanager.yml");
        }
        if (!file.exists()) {
            plugin.saveResource("modulemanager.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static void markEnabled(String name) {
        config.set("enabled-modules." + name.toLowerCase(), true);
        save();
    }

    public static void markDisabled(String name) {
        config.set("enabled-modules." + name.toLowerCase(), false);
        save();
    }

    public static boolean isModuleEnabled(String name) {
        if (config == null) reload();
        return config.getBoolean("enabled-modules." + name.toLowerCase(), false);
    }


    public static boolean shouldEnable(String name) {
        return isModuleEnabled(name);
    }


    private static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save modulemanager.yml: " + e.getMessage());
        }
    }
}
