package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CelestialPointsAdminCommand implements CommandExecutor, TabCompleter {

    private final CelestialPointManager pointManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CelestialPointsAdminCommand(CelestialPointManager pointManager) {
        this.pointManager = pointManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!ModuleManager.isEnabled("activity")) return true;
        if (!sender.hasPermission("celestialpoints.admin")) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.no-permission")));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.admin-usage").replace("%label%", label)));
            return true;
        }

        String action = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.invalid-amount")));
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.invalid-amount")));
            return true;
        }

        switch (action) {
            case "givepoints" -> {
                int newBalance = pointManager.addPoints(target, amount);
                sender.sendMessage(mm.deserialize(
                        MessageConfig.get("activity-reward.admin-give")
                                .replace("%player%", target.getName() != null ? target.getName() : "Unknown")
                                .replace("%amount%", Integer.toString(amount))
                                .replace("%balance%", Integer.toString(newBalance))
                ));
            }
            case "removepoints" -> {
                int newBalance = pointManager.removePoints(target, amount);
                sender.sendMessage(mm.deserialize(
                        MessageConfig.get("activity-reward.admin-remove")
                                .replace("%player%", target.getName() != null ? target.getName() : "Unknown")
                                .replace("%amount%", Integer.toString(amount))
                                .replace("%balance%", Integer.toString(newBalance))
                ));
            }
            case "setpoints" -> {
                pointManager.setPoints(target, amount);
                sender.sendMessage(mm.deserialize(
                        MessageConfig.get("activity-reward.admin-set")
                                .replace("%player%", target.getName() != null ? target.getName() : "Unknown")
                                .replace("%amount%", Integer.toString(amount))
                ));
            }
            default -> sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.unknown-subcommand")));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return switch (args.length) {
            case 1 -> Stream.of("givepoints", "removepoints", "setpoints")
                    .filter(opt -> opt.startsWith(args[0]))
                    .toList();
            case 2 -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(args[1]))
                    .toList();
            default -> Collections.emptyList();
        };
    }
}
