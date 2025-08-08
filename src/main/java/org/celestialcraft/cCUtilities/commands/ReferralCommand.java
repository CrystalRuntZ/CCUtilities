package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.referral.ReferralDatabase;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReferralCommand implements CommandExecutor {

    private final ReferralDatabase storage;
    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ReferralCommand(JavaPlugin plugin, ReferralDatabase storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("referral")) return true;

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<red>Usage: /referral <top|lookup <player>|<referrer>>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "top" -> {
                handleTop(sender);
                return true;
            }
            case "lookup" -> {
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /referral lookup <player>"));
                    return true;
                }
                handleLookup(sender, args[1]);
                return true;
            }
            default -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize("<red>Only players can submit referrals."));
                    return true;
                }

                String referrerName = args[0];

                if (storage.hasReferred(player.getUniqueId())) {
                    sender.sendMessage(mm.deserialize("<red>You have already submitted a referral."));
                    return true;
                }

                long joinTime = player.getFirstPlayed();
                long now = System.currentTimeMillis();
                long daysSinceJoin = ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(joinTime).atZone(ZoneOffset.UTC),
                        Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC)
                );

                if (daysSinceJoin > 7) {
                    sender.sendMessage(mm.deserialize("<red>You can only submit a referral within your first 7 days."));
                    return true;
                }

                boolean success = storage.submitReferral(player.getUniqueId(), player.getName(), referrerName);
                if (success) {
                    sender.sendMessage(mm.deserialize("<green>Thank you! You have been referred by <aqua>" + referrerName + "</aqua>."));
                    plugin.getLogger().info("[Referral] " + player.getName() + " was referred by " + referrerName);
                } else {
                    sender.sendMessage(mm.deserialize("<red>Referral submission failed. You may have already been referred."));
                }
                return true;
            }
        }
    }

    private void handleTop(CommandSender sender) {
        Map<UUID, Integer> allReferrals = storage.getAllReferralCounts();
        List<Map.Entry<UUID, Integer>> sorted = allReferrals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .toList();

        sender.sendMessage(mm.deserialize("<gold>Top Referrers:</gold>"));
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(entry.getKey()).getName()).orElse("Unknown");
            sender.sendMessage(mm.deserialize("<gray>" + rank + ". <aqua>" + name + " <white>- <green>" + entry.getValue() + " referrals"));
            rank++;
        }
    }

    private void handleLookup(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(mm.deserialize("<red>Player not found."));
            return;
        }

        int count = storage.getReferralCount(target.getUniqueId());
        sender.sendMessage(mm.deserialize("<aqua>" + target.getName() + " <white>has <green>" + count + " <white>referrals."));
    }
}
