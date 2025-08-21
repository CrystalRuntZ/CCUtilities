package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopSelectionStorage;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopResetter;
import org.celestialcraft.cCUtilities.utils.ShopUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public class ShopsMainCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final int SNAPSHOT_BLOCKS_PER_TICK = 8000;

    private Plugin plugin() {
        return CCUtilities.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("playershops")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("playershops.only-players")));
            return true;
        }

        if (label.equalsIgnoreCase("setshop")) {
            return handleSetWarp(player);
        }

        if (args.length == 0) {
            player.sendMessage(mm.deserialize("<red>Usage: /shops <trust|untrust|claim|unclaim|define|info|setwarp|warp|wand|delete>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        ShopRegion region = ShopDataManager.getRegionAt(player.getLocation());
        UUID playerId = player.getUniqueId();

        switch (sub) {
            case "wand" -> {
                if (!player.isOp()) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.unclaim-no-permission")));
                    return true;
                }
                ShopUtils.giveSelectionWand(player);
                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.wand-given")));
                return true;
            }

            case "trust" -> {
                if (args.length == 1) {
                    if (region == null || !Objects.equals(ShopDataManager.getOwnerUUID(region.name()), playerId)) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-not-owner-region")));
                        return true;
                    }
                    Set<UUID> trusted = ShopDataManager.getTrusted(region.name());
                    if (trusted.isEmpty()) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-list-empty")));
                    } else {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-list-header")));
                        for (UUID id : trusted) {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                            String name = op.getName() != null ? op.getName() : id.toString();
                            player.sendMessage(mm.deserialize(
                                    MessageConfig.get("playershops.trust-list-entry").replace("%player%", name)));
                        }
                    }
                    return true;
                } else {
                    if (region == null || !Objects.equals(ShopDataManager.getOwnerUUID(region.name()), playerId)) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-not-owner-region")));
                        return true;
                    }

                    UUID targetUUID;
                    String targetName;

                    Player online = Bukkit.getPlayerExact(args[1]);
                    if (online != null) {
                        targetUUID = online.getUniqueId();
                        targetName = online.getName();
                        if (ShopDataManager.isDivine(region) && !online.hasPermission("shops.claim.divine")) {
                            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-divine-permission")));
                            return true;
                        }
                    } else {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
                        if (off.isOnline() || off.hasPlayedBefore()) {
                            targetUUID = off.getUniqueId();
                            targetName = off.getName() != null ? off.getName() : args[1];
                        } else {
                            player.sendMessage(mm.deserialize(
                                    MessageConfig.get("playershops.player-not-online").replace("%player%", args[1])
                            ));
                            return true;
                        }
                    }

                    Set<UUID> trusted = ShopDataManager.getTrusted(region.name());
                    if (trusted.contains(targetUUID)) {
                        player.sendMessage(mm.deserialize(
                                MessageConfig.get("playershops.trust-already").replace("%player%", targetName)
                        ));
                        return true;
                    }

                    ShopDataManager.addTrusted(region.name(), targetUUID);
                    player.sendMessage(mm.deserialize(
                            MessageConfig.get("playershops.trust-success").replace("%player%", targetName)
                    ));
                    return true;
                }
            }

            case "untrust" -> {
                if (region == null || !Objects.equals(ShopDataManager.getOwnerUUID(region.name()), playerId)) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.trust-not-owner-region")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.untrust-usage")));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (ShopDataManager.isTrusted(region.name(), target.getUniqueId())) {
                    ShopDataManager.removeTrusted(region.name(), target.getUniqueId());
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.untrust-success")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[1]))));
                } else {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.untrust-fail")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[1]))));
                }
                return true;
            }

            case "info" -> {
                if (region == null) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.info-not-in-claimed-region")));
                    return true;
                }
                if (!ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize("<red>This shop is not claimed."));
                    return true;
                }
                UUID ownerUUID = ShopDataManager.getOwnerUUID(region.name());
                String ownerName = ownerUUID == null ? "Unknown"
                        : Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName()).orElse("Unknown");
                long lastUpdated = Optional.ofNullable(ShopDataManager.getLastUpdated(region.name())).orElse(0L);
                long delta = Math.max(0L, System.currentTimeMillis() - lastUpdated);
                long days = delta / (1000L * 60 * 60 * 24);
                long minutes = (delta / (1000L * 60)) % 60;

                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.info-header")));
                player.sendMessage(mm.deserialize(
                        MessageConfig.get("playershops.info-owner").replace("%owner%", ownerName)));
                player.sendMessage(mm.deserialize(
                        MessageConfig.get("playershops.info-last-updated")
                                .replace("%time%", days + " days, " + minutes + " minutes ago")));

                if (player.hasPermission("shops.info.admin")) {
                    String type;
                    if (ShopDataManager.isDivine(region)) {
                        type = "divine";
                    } else {
                        String rn = region.name().toLowerCase(Locale.ROOT);
                        if (rn.startsWith("saturn") || rn.startsWith("jupiter")) type = "saturn";
                        else if (rn.startsWith("ascendant")) type = "ascendant";
                        else type = "normal";
                    }
                    String tmpl = MessageConfig.get("playershops.info-type");
                    if (tmpl != null && !tmpl.isEmpty()) {
                        player.sendMessage(mm.deserialize(tmpl.replace("%type%", type)));
                    } else {
                        player.sendMessage(mm.deserialize("<#c1adfe>Type:</#c1adfe> <gray>" + type + "</gray>"));
                    }
                }
                return true;
            }

            case "claim" -> {
                // ADMIN: /shops claim <player>
                if (args.length >= 2 && player.hasPermission("shops.admin")) {
                    if (region == null) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-not-in-region")));
                        return true;
                    }
                    if (ShopDataManager.isClaimed(region.name())) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-already")));
                        return true;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    UUID targetId = target.getUniqueId();

                    ShopDataManager.claimShop(targetId, region.name());
                    player.sendMessage(mm.deserialize(
                            MessageConfig.get("playershops.claim-success").replace("%name%", region.name())));

                    // snapshot using player's current world (no reflection)
                    snapshotShopRegionAsync(region, player.getWorld());
                    return true;
                }

                // Normal claim
                if (region == null) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-not-in-region")));
                    return true;
                }
                if (!player.hasPermission("shops.claim")) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-no-permission")));
                    return true;
                }
                if (ShopDataManager.hasClaimed(playerId)) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-already-own")));
                    return true;
                }
                if (ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-already")));
                    return true;
                }
                if (ShopDataManager.isDivine(region) && !player.hasPermission("shops.claim.divine")) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-divine-no-permission")));
                    return true;
                }

                String rname = region.name().toLowerCase(Locale.ROOT);
                if ((rname.startsWith("saturn") || rname.startsWith("jupiter"))
                        && !player.hasPermission("shops.claim.saturn")) {
                    // use a new saturn message key (see messages step below)
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-saturn-no-permission")));
                    return true;
                }
                if (rname.startsWith("ascendant") && !player.hasPermission("shops.claim.ascendant")) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.claim-ascendant-no-permission")));
                    return true;
                }

                ShopDataManager.claimShop(playerId, region.name());
                player.sendMessage(mm.deserialize(
                        MessageConfig.get("playershops.claim-success").replace("%name%", region.name())));

                // snapshot using player's current world (no reflection)
                snapshotShopRegionAsync(region, player.getWorld());
                return true;
            }

            case "unclaim" -> {
                if (region == null || !ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.unclaim-fail")));
                    return true;
                }
                UUID owner = ShopDataManager.getOwnerUUID(region.name());
                boolean isOwner = owner != null && owner.equals(player.getUniqueId());
                if (!isOwner && !player.hasPermission("shops.admin")) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.unclaim-no-permission")));
                    return true;
                }

                player.sendMessage(mm.deserialize("<gray>Resetting shop plot…</gray>"));
                // pass the world hint explicitly
                new ShopResetter(plugin()).reset(region, player.getWorld(), () -> {
                    boolean ok = ShopDataManager.unclaimShopAt(player.getLocation());
                    ShopSelectionStorage.clear(player.getUniqueId());
                    if (ok) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.unclaim-success")));
                    } else {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.action-failed")));
                    }
                });
                return true;
            }

            case "define" -> {
                if (!player.isOp()) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-define-no-permission")));
                    return true;
                }
                String type = (args.length >= 2) ? args[1].toLowerCase(Locale.ROOT) : "plot";
                // switch
                switch (type) {
                    case "plot", "divine", "saturn", "ascendant" -> {}
                    default -> type = "plot";
                }
                ShopDataManager.defineShopRegionType(player, type);
                return true;
            }

            case "setwarp" -> {
                return handleSetWarp(player);
            }

            case "warp" -> {
                if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
                    Location warp = ShopDataManager.getRandomWarpActive(14);
                    if (warp == null) {
                        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-random-none-active")));
                        return true;
                    }
                    player.teleportAsync(warp).thenRun(() ->
                            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-random-success")))
                    );
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-usage")));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String shop = ShopDataManager.getClaim(target.getUniqueId());
                if (shop == null) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-no-claim")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[1]))));
                    return true;
                }
                Location warp = ShopDataManager.getWarp(shop);
                if (warp == null) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-no-warp")
                            .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[1]))));
                    return true;
                }
                player.teleportAsync(warp).thenRun(() ->
                        sender.sendMessage(mm.deserialize(MessageConfig.get("playershops.warp-success")
                                .replace("%player%", Optional.ofNullable(target.getName()).orElse(args[1]))))
                );
                return true;
            }

            case "delete" -> {
                if (!player.isOp()) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-delete-no-permission")));
                    return true;
                }
                if (region == null) {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-delete-not-in-region")));
                    return true;
                }
                if (ShopDataManager.isClaimed(region.name())) {
                    ShopDataManager.unclaimShopAt(player.getLocation());
                }
                boolean ok = ShopDataManager.deleteRegion(region.name());
                if (ok) {
                    ShopSelectionStorage.clear(player.getUniqueId());
                    player.sendMessage(mm.deserialize(
                            MessageConfig.get("playershops.region-delete-success").replace("%name%", region.name())));
                } else {
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-delete-fail")));
                }
                return true;
            }

            default -> {
                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.unknown-subcommand")));
                return true;
            }
        }
    }

    private boolean handleSetWarp(Player player) {
        ShopRegion region = ShopDataManager.getRegionAt(player.getLocation());
        if (region == null || !Objects.equals(ShopDataManager.getOwnerUUID(region.name()), player.getUniqueId())) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.setwarp-missing-claim")));
            return true;
        }
        Location loc = player.getLocation();
        ShopDataManager.setWarp(region.name(), loc);
        ShopDataManager.setLastUpdated(region.name(), System.currentTimeMillis());
        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.setwarp-success")));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            return Stream.of("trust", "untrust", "info", "claim", "unclaim", "define", "setwarp", "warp", "wand", "delete")
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim") && sender.hasPermission("shops.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("untrust") && sender instanceof Player p) {
            ShopRegion region = ShopDataManager.getRegionAt(p.getLocation());
            if (region != null && Objects.equals(ShopDataManager.getOwnerUUID(region.name()), p.getUniqueId())) {
                return ShopDataManager.getTrusted(region.name()).stream()
                        .map(uuid -> Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString()))
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("define")) {
            return Stream.of("plot", "divine", "saturn", "ascendant")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("warp")) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            names.add("random");
            return names;
        }
        return Collections.emptyList();
    }

    /* ============================ SNAPSHOT WRITER (SHOPS) ============================ */

    private void snapshotShopRegionAsync(ShopRegion r, World world) {
        if (r == null || r.min() == null || r.max() == null) return;

        World w = (world != null) ? world : r.min().getWorld();
        if (w == null) return;

        final int minX = Math.min(r.min().getBlockX(), r.max().getBlockX());
        final int maxX = Math.max(r.min().getBlockX(), r.max().getBlockX());
        final int minY = Math.min(r.min().getBlockY(), r.max().getBlockY());
        final int maxY = Math.max(r.min().getBlockY(), r.max().getBlockY());
        final int minZ = Math.min(r.min().getBlockZ(), r.max().getBlockZ());
        final int maxZ = Math.max(r.min().getBlockZ(), r.max().getBlockZ());
        final String regionName = r.name();

        final String header =
                "WORLD:" + w.getName() + "\n" +
                        "MINX:" + minX + "\n" +
                        "MAXX:" + maxX + "\n" +
                        "MINY:" + minY + "\n" +
                        "MAXY:" + maxY + "\n" +
                        "MINZ:" + minZ + "\n" +
                        "MAXZ:" + maxZ + "\n" +
                        "FORMAT:RLE-BLOCKDATA-1\n";

        final java.util.List<String> rleLines = new java.util.ArrayList<>();

        new BukkitRunnable() {
            int x = minX, y = minY, z = minZ;
            String current = null;
            int run = 0;

            @Override
            public void run() {
                int ops = 0;
                while (ops < SNAPSHOT_BLOCKS_PER_TICK) {
                    if (y > maxY) {
                        if (run > 0 && current != null) rleLines.add(run + "|" + current);
                        cancel();
                        Bukkit.getScheduler().runTaskAsynchronously(plugin(), () ->
                                writeShopSnapshotFile(regionName, header, rleLines)
                        );
                        return;
                    }
                    Block b = w.getBlockAt(x, y, z);
                    String dataStr = b.getBlockData().getAsString();

                    if (current == null) {
                        current = dataStr;
                        run = 1;
                    } else if (current.equals(dataStr)) {
                        run++;
                    } else {
                        rleLines.add(run + "|" + current);
                        current = dataStr;
                        run = 1;
                    }

                    ops++;
                    z++;
                    if (z > maxZ) {
                        z = minZ;
                        x++;
                    }
                    if (x > maxX) {
                        x = minX;
                        y++;
                    }
                }
            }
        }.runTaskTimer(plugin(), 1L, 1L);
    }

    private void writeShopSnapshotFile(String regionName, String header, List<String> rleLines) {
        Path dir = plugin().getDataFolder().toPath().resolve("shop_snapshots");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            plugin().getLogger().warning("[Shops] Could not create snapshots directory: " + dir + " (" + e.getMessage() + ")");
            return;
        }

        Path tmp = dir.resolve(regionName + ".snap.gz.tmp");
        Path out = dir.resolve(regionName + ".snap.gz");

        try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(tmp));
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8))) {
            w.write(header);
            for (String line : rleLines) {
                w.write(line);
                w.write('\n');
            }
        } catch (Exception ex) {
            plugin().getLogger().warning("[Shops] Snapshot write failed for " + regionName + ": " + ex.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {
            }
            return;
        }

        try {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            plugin().getLogger().info("[Shops] Snapshot saved: " + out.getFileName());
        } catch (Exception moveEx) {
            try {
                Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                plugin().getLogger().info("[Shops] Snapshot saved (non-atomic): " + out.getFileName());
            } catch (Exception ex) {
                plugin().getLogger().warning("[Shops] Snapshot move failed for " + regionName + ": " + ex.getMessage());
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                }
            }
        }
    }
}