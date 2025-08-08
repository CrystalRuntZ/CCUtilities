package org.celestialcraft.cCUtilities.modules.celestialvoting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.celestialvoting.config.VotingConfig;

import java.util.*;

public class RewardManager {

    private final VotingConfig config;
    private final Random random = new Random();

    public RewardManager(VotingConfig config) {
        this.config = config;
    }

    public void giveVoteReward(Player player) {
        String tier = getPermissionTier(player);
        if (tier == null) {
            return;
        }

        List<Map<String, Object>> pool;

        if (config.isBeginningOfMonth() && player.hasPermission("celestialvoting.botm." + tier)) {
            pool = config.getBotmRewardsForTier(tier);
        } else {
            pool = config.getRewardsForTier(tier);
        }

        if (pool == null || pool.isEmpty()) {
            return;
        }

        // Calculate total weight
        int totalWeight = pool.stream()
                .mapToInt(r -> (int) r.getOrDefault("chance", 0))
                .sum();

        if (totalWeight <= 0) {
            return;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (Map<String, Object> reward : pool) {
            int chance = (int) reward.getOrDefault("chance", 0);
            cumulative += chance;
            if (roll < cumulative) {
                applyReward(player, reward);
                VoteStreakTracker.recordVote(player);
                return;
            }
        }
    }


    private void applyReward(Player player, Map<String, Object> reward) {
        String type = ((String) reward.get("type")).toUpperCase(Locale.ROOT);
        String broadcastKey = (String) reward.getOrDefault("broadcast-key", null);

        switch (type) {
            case "COMMAND" -> {
                String command = ((String) reward.get("value")).replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            case "ITEM" -> {
                String mat = (String) reward.get("value");
                int amt = (int) reward.getOrDefault("amount", 1);
                Material material = Material.matchMaterial(mat);
                if (material != null) {
                    ItemStack item = new ItemStack(material, amt);
                    player.getInventory().addItem(item);
                }
            }
            case "XP" -> {
                int levels = (int) reward.getOrDefault("amount", 10);
                player.giveExpLevels(levels);
            }
            case "MESSAGE_ONLY" -> {
                // just a broadcast
            }
        }

        if (broadcastKey != null) {
            String key = "celestialvoting.rewards.broadcasts." + broadcastKey;
            Bukkit.broadcast(MessageConfig.mm(key).replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(player.getName())
            ));
        }
    }

    private String getPermissionTier(Player player) {
        for (String tier : config.getVoteTiers()) {
            if (player.hasPermission("celestialvoting." + tier)) {
                return tier;
            }
        }
        return "normal";
    }

    public VotingConfig getVotingConfig() {
        return config;
    }
}
