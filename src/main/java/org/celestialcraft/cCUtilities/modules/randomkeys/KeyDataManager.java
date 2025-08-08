package org.celestialcraft.cCUtilities.modules.randomkeys;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KeyDataManager {
    private final JavaPlugin plugin;
    private final Set<UUID> receivedToday = new HashSet<>();
    private final File dataFile;
    private final YamlConfiguration config;

    public KeyDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "keydata.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    public boolean hasReceivedKey(UUID uuid) {
        return receivedToday.contains(uuid);
    }

    public void markReceived(UUID uuid) {
        receivedToday.add(uuid);
    }

    public void resetDaily() {
        receivedToday.clear();
        save();
        plugin.getLogger().info("RandomKeys: Daily key tracking reset.");
    }

    public void save() {
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : receivedToday) {
            uuidStrings.add(uuid.toString());
        }
        config.set("received", uuidStrings);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save keydata.yml: " + e.getMessage());
        }
    }

    private void load() {
        List<String> list = config.getStringList("received");
        for (String entry : list) {
            try {
                receivedToday.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
