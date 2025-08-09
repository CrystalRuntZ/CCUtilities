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
        Bukkit.getLogger().info("[CED][RW] Distribute start for " + type + " id=" + dragonId);

        Map<UUID, Double> allDamage = tracker.getAllDamagers(dragonId);
        if (allDamage == null || allDamage.isEmpty()) {
            Bukkit.getLogger().warning("[CED][RW] No damage map for " + dragonId);
            return;
        }

        List<UUID> sorted = allDamage.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Double> leaderboard = new LinkedHashMap<>();
        for (UUID uuid : sorted) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) leaderboard.put(p.getName(), allDamage.get(uuid));
        }
        config.displayLeaderboard(leaderboard);

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            UUID uuid = sorted.get(i);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            List<String> perPlace = config.getTopCommands(type, i + 1);
            Bukkit.getLogger().info("[CED][RW] top-" + (i + 1) + " size=" + perPlace.size());
            if (!perPlace.isEmpty()) {
                for (String c : perPlace) runCommand(c, player);
                continue;
            }

            List<String> flat = config.getTopCommands(type);
            Bukkit.getLogger().info("[CED][RW] top(flat) size=" + flat.size());
            if (!flat.isEmpty()) {
                int idx = Math.min(i, flat.size() - 1);
                runCommand(flat.get(idx), player);
            }
        }

        List<String> thresholdCommands = config.getThresholdCommands(type);
        Map<Double, Double> thresholds = config.getThresholdChances(type);
        Bukkit.getLogger().info("[CED][RW] thresholds: commands=" + (thresholdCommands == null ? -1 : thresholdCommands.size())
                + " chances=" + (thresholds == null ? -1 : thresholds.size()));
        if (thresholdCommands == null || thresholdCommands.isEmpty() || thresholds == null || thresholds.isEmpty()) return;

        for (Map.Entry<Double, Double> entry : thresholds.entrySet()) {
            double threshold = entry.getKey();
            double chance = entry.getValue();
            for (UUID uuid : allDamage.keySet()) {
                double damage = tracker.getDamage(dragonId, uuid);
                if (damage >= threshold && Math.random() < chance) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        for (String command : thresholdCommands) runCommand(command, player);
                    }
                }
            }
        }
    }

    private void runCommand(String raw, Player player) {
        if (raw == null || raw.isEmpty() || player == null) return;
        String cmd = raw.replace("%player%", player.getName());
        while (cmd.startsWith("/")) cmd = cmd.substring(1);
        Bukkit.getLogger().info("[CED][RW] console: " + cmd);
        boolean consoleOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        if (!consoleOk) {
            Bukkit.getLogger().warning("[CED][RW] console failed, fallback to player");
            player.performCommand(cmd);
        }
        if (MessageConfig.has("ced.reward-given")) player.sendMessage(mm.deserialize(MessageConfig.get("ced.reward-given")));
    }
}
