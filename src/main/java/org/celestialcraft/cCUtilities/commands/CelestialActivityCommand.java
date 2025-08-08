package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.PlayerActivityTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CelestialActivityCommand implements CommandExecutor, TabCompleter {

    private final CelestialPointManager pointManager;
    private final PlayerActivityTracker tracker;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public CelestialActivityCommand(CelestialPointManager pointManager, PlayerActivityTracker tracker) {
        this.pointManager = pointManager;
        this.tracker = tracker;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!ModuleManager.isEnabled("activity")) return true;

        if (args.length == 0) {
            if (sender instanceof Player player) {
                int balance = pointManager.getPoints(player);
                boolean active = System.currentTimeMillis() - tracker.getLastActive(player.getUniqueId()) <= 30 * 60 * 1000;

                Component statusComponent = mini.deserialize(MessageConfig.get(
                        active ? "activity-reward.status-active" : "activity-reward.status-idle"
                ));

                player.sendMessage(mini.deserialize(
                        MessageConfig.get("activity-reward.balance-self"),
                        Placeholder.component("balance", Component.text(balance)),
                        Placeholder.component("status", statusComponent)
                ));
            } else {
                sender.sendMessage(mini.deserialize(MessageConfig.get("activity-reward.usage")));
            }
            return true;
        }

        if (args.length == 1 && sender.hasPermission("celestialpoints.admin")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            int balance = pointManager.getPoints(target);
            boolean active = System.currentTimeMillis() - tracker.getLastActive(target.getUniqueId()) <= 30 * 60 * 1000;

            Component statusComponent = mini.deserialize(MessageConfig.get(
                    active ? "activity-reward.status-active" : "activity-reward.status-idle"
            ));

            sender.sendMessage(mini.deserialize(
                    MessageConfig.get("activity-reward.balance-other"),
                    Placeholder.unparsed("player", target.getName() != null ? target.getName() : "Unknown"),
                    Placeholder.component("balance", Component.text(balance)),
                    Placeholder.component("status", statusComponent)
            ));
            return true;
        }

        sender.sendMessage(mini.deserialize(MessageConfig.get("activity-reward.usage")));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (args.length == 1 && sender.hasPermission("celestialpoints.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
