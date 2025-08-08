package org.celestialcraft.cCUtilities.modules.referral;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ReferralModule implements CommandExecutor, Module {

    private final JavaPlugin plugin;
    private ReferralDatabase database;
    public ReferralLeaderboardGUI gui;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private boolean enabled = false;

    public ReferralModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        plugin.saveDefaultConfig();
        this.database = new ReferralDatabase(plugin);
        this.gui = new ReferralLeaderboardGUI(database);
        plugin.getLogger().info("Referral module enabled.");
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        plugin.getLogger().info("Referral module disabled.");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "referral";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!enabled) {
            sender.sendMessage(mini.deserialize("<red>This module is currently disabled.</red>"));
            return true;
        }

        if (args.length == 0) {
            sendFormatted(sender, "referral.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "top" -> {
                if (sender instanceof Player player) {
                    gui.open(player);
                } else {
                    Map<String, Integer> top = database.getTopReferrers(10);
                    if (top.isEmpty()) {
                        sendFormatted(sender, "referral.top-empty");
                        return true;
                    }
                    sender.sendMessage(mini.deserialize("<gold>Top Referrers:</gold>"));
                    int rank = 1;
                    for (Map.Entry<String, Integer> entry : top.entrySet()) {
                        sender.sendMessage(mini.deserialize("<gray>" + rank + ". <yellow>" + entry.getKey() +
                                "</yellow>: <aqua>" + entry.getValue() + "</aqua>"));
                        rank++;
                    }
                }
                return true;
            }

            case "lookup" -> {
                if (args.length != 2) {
                    sendFormatted(sender, "referral.usage");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String referrer = database.getReferrer(target.getUniqueId());
                List<String> referred = database.getReferredPlayers(args[1]);

                sender.sendMessage(mini.deserialize("<gold>Referral Lookup for " + target.getName() + ":</gold>"));

                if (referrer != null) {
                    sender.sendMessage(mini.deserialize("<gray>Referred by: <aqua>" + referrer + "</aqua>"));
                } else {
                    sender.sendMessage(mini.deserialize("<gray>Referred by: <red>None</red>"));
                }

                if (!referred.isEmpty()) {
                    sender.sendMessage(mini.deserialize("<gray>They referred:</gray>"));
                    for (String name : referred) {
                        sender.sendMessage(mini.deserialize("<yellow>- " + name));
                    }
                } else {
                    sender.sendMessage(mini.deserialize("<gray>They have not referred anyone."));
                }
                return true;
            }

            default -> {
                if (!(sender instanceof Player player)) {
                    sendFormatted(sender, "referral.only-players");
                    return true;
                }

                if (args.length != 1) {
                    sendFormatted(player, "referral.usage");
                    return true;
                }

                String referrerName = args[0];

                if (database.hasReferred(player.getUniqueId())) {
                    sendFormatted(player, "referral.already-referred");
                    return true;
                }

                long joinTime = player.getFirstPlayed();
                long now = System.currentTimeMillis();
                long daysSinceJoin = ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(joinTime).atZone(ZoneOffset.UTC),
                        Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC)
                );

                if (daysSinceJoin > 7) {
                    sendFormatted(player, "referral.too-late");
                    return true;
                }

                boolean success = database.submitReferral(player.getUniqueId(), player.getName(), referrerName);
                if (success) {
                    sendFormatted(player, "referral.success", Map.of("referrer", referrerName));
                    plugin.getLogger().info("[Referral] " + player.getName() + " referred by " + referrerName);
                } else {
                    sendFormatted(player, "referral.already-referred");
                }
                return true;
            }
        }
    }

    public ReferralDatabase getDatabase() {
        return database;
    }

    private void sendFormatted(CommandSender sender, String path) {
        sendFormatted(sender, path, Map.of());
    }

    private void sendFormatted(CommandSender sender, String path, Map<String, String> placeholders) {
        String resolved = MessageConfig.get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        sender.sendMessage(mini.deserialize(resolved));
    }
}
