package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.celestialvoting.RewardManager;
import org.celestialcraft.cCUtilities.modules.celestialvoting.VoteStreakTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CelestialVoteRewardCommand implements CommandExecutor, TabCompleter {

    private final RewardManager rewardManager;

    public CelestialVoteRewardCommand(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        try {
            Bukkit.getLogger().info("[DEBUG] /celestialvoting command executed: " + String.join(" ", args));

            if (!ModuleManager.isEnabled("celestialvoting")) {
                sender.sendMessage(MessageConfig.mm("celestialvoting.module-disabled"));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(MessageConfig.mm("celestialvoting.command-usage"));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reward" -> {
                    if (args.length != 2) {
                        sender.sendMessage(MessageConfig.mm("celestialvoting.reward-command-usage"));
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !target.isOnline()) {
                        sender.sendMessage(MessageConfig.mm("celestialvoting.player-not-found"));
                        return true;
                    }

                    Bukkit.getLogger().info("[DEBUG] Giving vote reward to " + target.getName());
                    rewardManager.giveVoteReward(target);
                    return true;
                }

                case "reload" -> {
                    if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("celestialvoting.reload")) {
                        sender.sendMessage(MessageConfig.mm("celestialvoting.no-permission"));
                        return true;
                    }

                    CCUtilities.getInstance().votingModule.reload();
                    sender.sendMessage(MessageConfig.mm("celestialvoting.reload-success"));
                    return true;
                }

                case "streak" -> {
                    if (args.length == 1) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(MessageConfig.mm("celestialvoting.streak-console-usage"));
                            return true;
                        }
                        if (!player.hasPermission("celestialvoting.streak")) {
                            sender.sendMessage(MessageConfig.mm("celestialvoting.streak-no-permission"));
                            return true;
                        }

                        int streak = VoteStreakTracker.getCurrentStreak(player.getUniqueId());
                        sender.sendMessage(MessageConfig.mm("celestialvoting.streak-self")
                                .replaceText(builder -> builder.matchLiteral("%streak%").replacement(String.valueOf(streak))));
                        return true;
                    }

                    if (args.length == 2) {
                        if (!sender.hasPermission("celestialvoting.streak.others")) {
                            sender.sendMessage(MessageConfig.mm("celestialvoting.streak-no-permission"));
                            return true;
                        }

                        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                        int streak = VoteStreakTracker.getCurrentStreak(target.getUniqueId());

                        sender.sendMessage(MessageConfig.mm("celestialvoting.streak-other")
                                .replaceText(builder -> builder
                                        .matchLiteral("%player%").replacement(target.getName() != null ? target.getName() : "Unknown")
                                        .matchLiteral("%streak%").replacement(String.valueOf(streak))));
                        return true;
                    }

                    sender.sendMessage(MessageConfig.mm("celestialvoting.command-usage"));
                    return true;
                }

                default -> {
                    sender.sendMessage(MessageConfig.mm("celestialvoting.command-usage"));
                    return true;
                }
            }

        } catch (Exception e) {
            sender.sendMessage(Component.text("§cAn internal error occurred. Check console for details."));
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("reward", "reload", "streak");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("streak") && sender.isOp()) {
            List<String> players = new ArrayList<>();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.getName() != null) players.add(player.getName());
            }
            return players;
        }
        return Collections.emptyList();
    }
}
