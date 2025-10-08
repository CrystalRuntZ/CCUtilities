package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopWarpCommand implements CommandExecutor, TabCompleter {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final Map<UUID, Long> lastWarp = new ConcurrentHashMap<>();
    private static final int COOLDOWN_SECONDS = 3; // was 5
    private static final int ACTIVE_DAYS = 14;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("playershops")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-only-players")));
            return true;
        }

        // PvP-tag check (expects a scoreboard tag "pvp-tagged" or metadata "pvpTagged")
        if (isPvPTagged(player)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-pvp-tagged")));
            return true;
        }

        // cooldown
        long now = System.currentTimeMillis();
        long last = lastWarp.getOrDefault(player.getUniqueId(), 0L);
        long remaining = COOLDOWN_SECONDS * 1000L - (now - last);
        if (remaining > 0) {
            long secs = (remaining + 999) / 1000;
            player.sendMessage(mm.deserialize(
                    MessageConfig.get("playershops.warp-cooldown").replace("%seconds%", String.valueOf(secs))
            ));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-usage")));
            return true;
        }

        if (args[0].equalsIgnoreCase("random")) {
            Location warp = ShopDataManager.getRandomWarpActive(ACTIVE_DAYS);
            if (warp == null) {
                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-random-none-active")));
                return true;
            }
            lastWarp.put(player.getUniqueId(), now);
            player.teleportAsync(warp).thenRun(() ->
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-random-success")))
            );
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID tid = target.getUniqueId();

        String shop = ShopDataManager.getClaim(tid);
        if (shop == null) {
            player.sendMessage(mm.deserialize(
                    MessageConfig.get("playershops.warp-no-claim")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[0]))
            ));
            return true;
        }

        Location warp = ShopDataManager.getWarp(shop);
        if (warp == null) {
            player.sendMessage(mm.deserialize(
                    MessageConfig.get("playershops.warp-no-warp")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[0]))
            ));
            return true;
        }

        lastWarp.put(player.getUniqueId(), now);
        player.teleportAsync(warp).thenRun(() ->
                player.sendMessage(mm.deserialize(
                        MessageConfig.get("playershops.warp-success")
                                .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[0]))
                ))
        );
        return true;
    }

    private boolean isPvPTagged(Player p) {
        if (p.getScoreboardTags().contains("pvp-tagged")) return true;
        if (p.hasMetadata("pvpTagged")) {
            return p.getMetadata("pvpTagged").stream().anyMatch(mv -> {
                Object v = mv.value();
                return (v instanceof Boolean b && b) || (v instanceof String s && Boolean.parseBoolean(s));
            });
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            out.add("random");
            String pref = args[0].toLowerCase();
            out.removeIf(s -> !s.toLowerCase().startsWith(pref));
            return out;
        }
        return Collections.emptyList();
    }
}
