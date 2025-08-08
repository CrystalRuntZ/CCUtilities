package org.celestialcraft.cCUtilities.modules.ced;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.*;

public class RewardDistributor {

    private final DragonConfig config;
    private final DamageTracker tracker;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RewardDistributor(DragonConfig config, DamageTracker tracker) {
        this.config = config;
        this.tracker = tracker;
    }

    public void distribute(EnderDragon dragon, DragonType type, UUID dragonId) {
        Bukkit.getLogger().info("[CED] Starting reward distribution for dragon: " + dragonId);

        Map<UUID, Double> allDamage = tracker.getAllDamagers(dragonId);
        if (allDamage == null || allDamage.isEmpty()) {
            Bukkit.getLogger().warning("[CED] No damage data found for dragon: " + dragonId);
            return;
        }

        Bukkit.getLogger().info("[CED] Total players with damage: " + allDamage.size());

        List<UUID> sorted = allDamage.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Double> leaderboard = new LinkedHashMap<>();
        for (UUID uuid : sorted) {
            Player player = Bukkit.getPlayer(uuid);
            double dmg = allDamage.get(uuid);
            if (player != null) {
                leaderboard.put(player.getName(), dmg);
                Bukkit.getLogger().info("[CED] Leaderboard entry: " + player.getName() + " -> " + dmg);
            } else {
                Bukkit.getLogger().warning("[CED] Player with UUID " + uuid + " not online for leaderboard.");
            }
        }

        config.displayLeaderboard(leaderboard);

        List<String> topCommands = config.getTopCommands(type);
        if (topCommands == null || topCommands.isEmpty()) {
            Bukkit.getLogger().warning("[CED] No top reward commands found for dragon type: " + type);
        } else {
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                UUID uuid = sorted.get(i);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    Bukkit.getLogger().info("[CED] Granting top " + (i + 1) + " reward to: " + player.getName());
                    for (String command : topCommands) {
                        runCommand(command, player);
                    }
                } else {
                    Bukkit.getLogger().warning("[CED] Top " + (i + 1) + " player is offline: " + uuid);
                }
            }
        }

        List<String> thresholdCommands = config.getThresholdCommands(type);
        Map<Double, Double> thresholds = config.getThresholdChances(type);

        if (thresholdCommands == null || thresholdCommands.isEmpty()) {
            Bukkit.getLogger().warning("[CED] No threshold reward commands for dragon type: " + type);
            return;
        }

        if (thresholds == null || thresholds.isEmpty()) {
            Bukkit.getLogger().warning("[CED] No thresholds defined for dragon type: " + type);
            return;
        }

        Bukkit.getLogger().info("[CED] Checking threshold-based rewards...");

        for (Map.Entry<Double, Double> entry : thresholds.entrySet()) {
            double threshold = entry.getKey();
            double chance = entry.getValue();

            Bukkit.getLogger().info("[CED] Threshold " + threshold + ", chance " + chance);

            for (UUID uuid : allDamage.keySet()) {
                double damage = tracker.getDamage(dragonId, uuid);
                Bukkit.getLogger().info("[CED] Player " + uuid + " dealt " + damage + " damage");

                if (damage >= threshold) {
                    Bukkit.getLogger().info("[CED] Player " + uuid + " passed threshold " + threshold);

                    if (Math.random() < chance) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            Bukkit.getLogger().info("[CED] Threshold reward granted to: " + player.getName());
                            for (String command : thresholdCommands) {
                                runCommand(command, player);
                            }
                        } else {
                            Bukkit.getLogger().warning("[CED] Threshold-eligible player is offline: " + uuid);
                        }
                    } else {
                        Bukkit.getLogger().info("[CED] Player " + uuid + " failed chance roll");
                    }
                }
            }
        }
    }

    private void runCommand(String raw, Player player) {
        if (raw == null || raw.isEmpty() || player == null) {
            Bukkit.getLogger().warning("[CED] Skipped empty/null command or player");
            return;
        }

        String command = raw.replace("%player%", player.getName());
        Bukkit.getLogger().info("[CED] Dispatching: " + command);
        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        if (success) {
            Bukkit.getLogger().info("[CED] Command executed successfully: " + command);
        } else {
            Bukkit.getLogger().warning("[CED] Command failed: " + command);
        }

        if (MessageConfig.has("ced.reward-given")) {
            player.sendMessage(mm.deserialize(MessageConfig.get("ced.reward-given")));
        }
    }
}
