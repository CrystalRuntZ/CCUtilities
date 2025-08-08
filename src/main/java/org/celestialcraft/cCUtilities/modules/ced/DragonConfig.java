package org.celestialcraft.cCUtilities.modules.ced;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.io.File;
import java.util.*;

public class DragonConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public DragonConfig(FileConfiguration config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "CustomEnderDragon.yml");
        if (!file.exists()) {
            plugin.saveResource("CustomEnderDragon.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        Bukkit.getLogger().info("[CED] Reloaded CustomEnderDragon.yml");
    }

    public String getName(DragonType type) {
        return config.getString("dragon-types." + type.name() + ".name", type.name());
    }

    public Component getColoredName(DragonType type) {
        String key = "ced.dragon-names." + type.name().toLowerCase();
        return MessageConfig.has(key)
                ? MessageConfig.mm(key)
                : Component.text(getName(type));
    }

    public double getBaseHealth(DragonType type) {
        return config.getDouble("dragon-types." + type.name() + ".base-health", 200.0);
    }

    public double getHealthPerPlayer(DragonType type) {
        return config.getDouble("dragon-types." + type.name() + ".health-per-player", 50.0);
    }

    public BarColor getBossBarColor(DragonType type) {
        String raw = config.getString("dragon-types." + type.name() + ".bossbar-color", "PURPLE").toUpperCase(Locale.ROOT);
        try {
            return BarColor.valueOf(raw);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[CED] Invalid bossbar-color '" + raw + "' for dragon " + type.name() + ", defaulting to PURPLE.");
            return BarColor.PURPLE;
        }
    }

    public void displayLeaderboard(Map<String, Double> damageMap) {
        if (damageMap.isEmpty()) {
            Bukkit.broadcast(MessageConfig.mm("<red>No leaderboard data to display."));
            return;
        }

        List<Map.Entry<String, Double>> sortedEntries = damageMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .toList();

        if (MessageConfig.has("ced.leaderboard.header"))
            Bukkit.broadcast(MessageConfig.mm("ced.leaderboard.header"));

        int position = 1;
        for (Map.Entry<String, Double> entry : sortedEntries) {
            final int place = position;
            Bukkit.broadcast(
                    MessageConfig.mm("ced.leaderboard.entry")
                            .replaceText(b -> b.matchLiteral("%place%").replacement(String.valueOf(place)))
                            .replaceText(b -> b.matchLiteral("%player%").replacement(entry.getKey()))
                            .replaceText(b -> b.matchLiteral("%damage%").replacement(String.format("%.2f", entry.getValue()))));
            position++;
        }

        if (MessageConfig.has("ced.leaderboard.footer"))
            Bukkit.broadcast(MessageConfig.mm("ced.leaderboard.footer"));
    }

    public List<String> getTopCommands(DragonType type) {
        String path = "dragons." + type.name() + ".rewards.top";
        if (!config.isList(path)) {
            Bukkit.getLogger().warning("[CED] No top reward commands found at path: " + path);
            return Collections.emptyList();
        }
        return config.getStringList(path);
    }

    public List<String> getThresholdCommands(DragonType type) {
        String path = "dragons." + type.name() + ".rewards.thresholds.commands";
        if (!config.isList(path)) {
            Bukkit.getLogger().warning("[CED] No threshold reward commands found at path: " + path);
            return Collections.emptyList();
        }
        return config.getStringList(path);
    }

    public Map<Double, Double> getThresholdChances(DragonType type) {
        Map<Double, Double> result = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("dragons." + type.name() + ".rewards.thresholds.chances");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    double threshold = Double.parseDouble(key);
                    double chance = section.getDouble(key);
                    result.put(threshold, chance);
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}
