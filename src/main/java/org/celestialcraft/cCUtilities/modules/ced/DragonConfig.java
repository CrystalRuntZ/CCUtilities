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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DragonConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public DragonConfig(FileConfiguration ignored, JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "CustomEnderDragon.yml");
        if (!file.exists()) {
            plugin.saveResource("CustomEnderDragon.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        Bukkit.getLogger().info("[CED] Reloaded CustomEnderDragon.yml");
    }

    private void debugPath(String label, String path) {
        boolean present = config.contains(path);
        Bukkit.getLogger().info("[CED][CFG] " + label + " path=" + path + " present=" + present);
    }

    public String getName(DragonType type) {
        String pathNew = "dragons." + type.name() + ".name";
        String pathOld = "dragon-types." + type.name() + ".name";
        if (config.contains(pathNew)) return Objects.toString(config.getString(pathNew), type.name());
        if (config.contains(pathOld)) return Objects.toString(config.getString(pathOld), type.name());
        return type.name();
    }

    public Component getColoredName(DragonType type) {
        String keyUpper = "ced.dragon-names." + type.name();
        String keyLower = "ced.dragon-names." + type.name().toLowerCase(Locale.ROOT);
        if (MessageConfig.has(keyUpper)) return MessageConfig.mm(keyUpper);
        if (MessageConfig.has(keyLower)) return MessageConfig.mm(keyLower);
        return Component.text(getName(type));
    }

    public double getBaseHealth(DragonType type) {
        String pathNew = "dragons." + type.name() + ".base-health";
        String pathOld = "dragon-types." + type.name() + ".base-health";
        debugPath("base-health:new", pathNew);
        debugPath("base-health:old", pathOld);
        if (config.contains(pathNew)) return config.getDouble(pathNew, 200.0);
        if (config.contains(pathOld)) return config.getDouble(pathOld, 200.0);
        return 200.0;
    }

    public double getHealthPerPlayer(DragonType type) {
        String pathNew = "dragons." + type.name() + ".health-per-player";
        String pathOld = "dragon-types." + type.name() + ".health-per-player";
        debugPath("hpp:new", pathNew);
        debugPath("hpp:old", pathOld);
        if (config.contains(pathNew)) return config.getDouble(pathNew, 50.0);
        if (config.contains(pathOld)) return config.getDouble(pathOld, 50.0);
        return 50.0;
    }

    public BarColor getBossBarColor(DragonType type) {
        String pathNew = "dragons." + type.name() + ".bossbar-color";
        String pathOld = "dragon-types." + type.name() + ".bossbar-color";
        debugPath("barcolor:new", pathNew);
        debugPath("barcolor:old", pathOld);
        String raw;
        if (config.contains(pathNew)) {
            raw = Objects.toString(config.getString(pathNew, "PURPLE"), "PURPLE");
        } else if (config.contains(pathOld)) {
            raw = Objects.toString(config.getString(pathOld, "PURPLE"), "PURPLE");
        } else {
            raw = "PURPLE";
        }
        try {
            return BarColor.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[CED] Invalid bossbar-color '" + raw + "' for " + type.name() + ", defaulting to PURPLE.");
            return BarColor.PURPLE;
        }
    }

    public java.util.List<String> getTopCommands(DragonType type) {
        String path = "dragons." + type.name() + ".rewards.top";
        debugPath("rewards:top(flat)", path);
        if (!config.isList(path)) return java.util.Collections.emptyList();
        return config.getStringList(path);
    }

    public java.util.List<String> getTopCommands(DragonType type, int place) {
        String path = "dragons." + type.name() + ".rewards.top-" + place;
        debugPath("rewards:top-" + place, path);
        if (!config.isList(path)) return java.util.Collections.emptyList();
        return config.getStringList(path);
    }

    public java.util.List<String> getThresholdCommands(DragonType type) {
        String path = "dragons." + type.name() + ".rewards.thresholds.commands";
        debugPath("rewards:threshold.commands", path);
        if (!config.isList(path)) return java.util.Collections.emptyList();
        return config.getStringList(path);
    }

    public Map<Double, Double> getThresholdChances(DragonType type) {
        Map<Double, Double> result = new HashMap<>();
        String base = "dragons." + type.name() + ".rewards.thresholds.chances";
        debugPath("rewards:threshold.chances", base);
        ConfigurationSection section = config.getConfigurationSection(base);
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

    public void displayLeaderboard(Map<String, Double> damageMap) {
        if (damageMap.isEmpty()) {
            Bukkit.broadcast(MessageConfig.mm("<red>No leaderboard data to display."));
            return;
        }
        var sorted = damageMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        if (MessageConfig.has("ced.leaderboard.header"))
            Bukkit.broadcast(MessageConfig.mm("ced.leaderboard.header"));

        int pos = 1;
        for (var e : sorted) {
            final int place = pos++;
            Bukkit.broadcast(
                    MessageConfig.mm("ced.leaderboard.entry")
                            .replaceText(b -> b.matchLiteral("%place%").replacement(String.valueOf(place)))
                            .replaceText(b -> b.matchLiteral("%player%").replacement(e.getKey()))
                            .replaceText(b -> b.matchLiteral("%damage%").replacement(String.format("%.2f", e.getValue()))));
        }

        if (MessageConfig.has("ced.leaderboard.footer"))
            Bukkit.broadcast(MessageConfig.mm("ced.leaderboard.footer"));
    }
}
