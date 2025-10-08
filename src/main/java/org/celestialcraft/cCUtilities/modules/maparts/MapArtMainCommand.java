package org.celestialcraft.cCUtilities.modules.maparts;

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
import org.celestialcraft.cCUtilities.MessageConfig;
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

public class MapArtMainCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final MapArtDataManager data;
    private final MiniMessage mini = MiniMessage.miniMessage();

    // tune how many blocks to record per tick when snapshotting
    private static final int SNAPSHOT_BLOCKS_PER_TICK = 8000;

    public MapArtMainCommand(Plugin plugin, MapArtDataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(mini.deserialize(msg("mapart.usage", "<gray>/mapart <wand|define|claim|unclaim|trust|untrust|lock|info|trustlist|addclaim|setclaims|setwarp|warp></gray>")));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "wand" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasAdmin(sender)) { sender.sendMessage(mini.deserialize(msg("mapart.no-permission","<red>No permission.</red>"))); return true; }
                MapArtSelectionManager.giveWand(p);
                return true;
            }

            case "addclaim" -> {
                if (!hasAdmin(sender)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.no-permission", "<red>No permission.</red>")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(mini.deserialize("<gray>/mapart addclaim <player></gray>"));
                    return true;
                }
                OfflinePlayer target = getOffline(args[1]);
                if (target == null || !(target.isOnline() || target.hasPlayedBefore())) {
                    sender.sendMessage(mini.deserialize(msg("mapart.no-such-player", "<red>Player not found.</red>")));
                    return true;
                }
                int newBonus = data.addOneBonusClaim(target.getUniqueId());
                String tName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(mini.deserialize(
                        "<green>Granted +1 mapart claim to </green><#c1adfe>"+tName+
                                "</#c1adfe><green>. Bonus now: </green><#c1adfe>"+newBonus+"</#c1adfe>"));
                return true;
            }

            case "info" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }
                var at = data.regionAt(p.getLocation());
                if (at == null) { sender.sendMessage(mini.deserialize(msg("mapart.info-not-in-region","<yellow>Stand inside a mapart region.</yellow>"))); return true; }
                String name = at.getName();
                if (!data.isClaimed(name)) { sender.sendMessage(mini.deserialize(msg("mapart.info-unclaimed","<yellow>This mapart is unclaimed.</yellow>"))); return true; }
                UUID ownerId = data.getClaimer(name);
                String ownerName = ownerId != null ? Bukkit.getOfflinePlayer(ownerId).getName() : "unknown";
                boolean locked = data.isLocked(name);
                sender.sendMessage(mini.deserialize("<#c1adfe>MapArt:</#c1adfe> " + name));
                sender.sendMessage(mini.deserialize("<gray>Owner:</gray> <#c1adfe>" + ownerName + "</#c1adfe>"));
                sender.sendMessage(mini.deserialize(locked ? "<gray>Status:</gray> <red>Locked</red>" : "<gray>Status:</gray> <green>Unlocked</green>"));
                return true;
            }

            case "trustlist" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }
                var at = data.regionAt(p.getLocation());
                if (at == null || !data.isClaimed(at.getName())) { sender.sendMessage(mini.deserialize(msg("mapart.not-in-claimed","<red>Stand inside your claimed mapart region.</red>"))); return true; }
                if (!data.isOwner(at.getName(), p.getUniqueId()) && !hasAdmin(p)) { sender.sendMessage(mini.deserialize(msg("mapart.only-owner","<red>Only the owner can do this.</red>"))); return true; }
                var uuids = data.getTrusted(at.getName());
                if (uuids.isEmpty()) { sender.sendMessage(mini.deserialize("<yellow>No trusted players.</yellow>")); return true; }
                List<String> names = new ArrayList<>();
                for (UUID id : uuids) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                    names.add(op.getName() == null ? id.toString() : op.getName());
                }
                sender.sendMessage(mini.deserialize("<gray>Trusted:</gray> <#c1adfe>" + String.join(", ", names) + "</#c1adfe>"));
                return true;
            }

            case "define" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>")));
                    return true;
                }
                if (!hasAdmin(sender)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.no-permission","<red>No permission.</red>")));
                    return true;
                }

                var pos1 = MapArtSelectionManager.getPos1(p);
                var pos2 = MapArtSelectionManager.getPos2(p);
                if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
                    sender.sendMessage(mini.deserialize(msg("mapart.define-no-positions","<red>Select two corners first.</red>")));
                    return true;
                }
                if (!Objects.equals(pos1.getWorld(), pos2.getWorld())) {
                    sender.sendMessage(mini.deserialize(msg("mapart.define-world-mismatch","<red>Both corners must be in the same world.</red>")));
                    return true;
                }

                World w = pos1.getWorld();

                // X/Z bounds from selection
                int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
                int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

                // Make region a full vertical column so standing on the platform counts
                int minY = w.getMinHeight();
                int maxY = w.getMaxHeight() - 1;

                Location c1 = new Location(w, minX, minY, minZ);
                Location c2 = new Location(w, maxX, maxY, maxZ);

                // Support explicit name OR auto-name if omitted
                String name;
                if (args.length >= 2) {
                    name = args[1];
                    if (data.regionExists(name)) {
                        sender.sendMessage(mini.deserialize(msg("mapart.define-exists","<red>A mapart with that name already exists.</red>")));
                        return true;
                    }
                } else {
                    // requires DataManager to provide nextAutoName(worldName)
                    name = data.nextAutoName(w.getName());
                }

                MapArtRegion region = new MapArtRegion(name, w, c1, c2);
                data.addRegion(region);
                MapArtSelectionManager.clear(p);

                sender.sendMessage(
                        mini.deserialize(msg("mapart.define-success","<green>Defined mapart region.</green>"))
                                .append(mini.deserialize(" <#c1adfe>" + name + "</#c1adfe>"))
                );
                return true;
            }

            case "claim" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }

                // Auto-claim mode
                if (args.length >= 2 && args[1].equalsIgnoreCase("auto")) {
                    int maxClaims = getMaxClaims(p);
                    if (maxClaims <= 0) { p.sendMessage(mini.deserialize(msg("mapart.claim-no-permission","<red>You cannot claim mapart regions.</red>"))); return true; }
                    int current = data.countClaims(p.getUniqueId());
                    if (current >= maxClaims) { p.sendMessage(mini.deserialize(msg("mapart.claim-limit","<red>You have reached your claim limit.</red>"))); return true; }

                    MapArtRegion target = findNearestUnclaimed(p.getLocation());
                    if (target == null) { p.sendMessage(mini.deserialize(msg("mapart.auto-none","<red>No unclaimed mapart regions are available.</red>"))); return true; }
                    if (!data.claim(target.getName(), p.getUniqueId())) { p.sendMessage(mini.deserialize(msg("mapart.claim-failed","<red>Claim failed.</red>"))); return true; }

                    // snapshot the plot as-claimed
                    snapshotRegionAsync(target);

                    // Teleport to the new platform center using helper + auto-set warp
                    Location spawn = computePlatformSpawn(target, p);
                    if (spawn != null) {
                        data.setWarp(target.getName(), spawn);
                        p.teleportAsync(spawn).thenRun(() ->
                                p.sendMessage(mini.deserialize(
                                        msg("mapart.claim-success","<green>Claimed mapart region:</green>")
                                ).append(mini.deserialize(" <#c1adfe>" + target.getName() + "</#c1adfe>")))
                        );
                    } else {
                        // fallback: world missing or null
                        p.sendMessage(mini.deserialize(
                                msg("mapart.claim-success","<green>Claimed mapart region:</green>")
                        ).append(mini.deserialize(" <#c1adfe>" + target.getName() + "</#c1adfe>")));
                    }
                    return true;
                }

                // Normal claim at current position
                int maxClaims = getMaxClaims(p);
                if (maxClaims <= 0) { sender.sendMessage(mini.deserialize(msg("mapart.claim-no-permission","<red>You cannot claim mapart regions.</red>"))); return true; }
                int current = data.countClaims(p.getUniqueId());
                if (current >= maxClaims) { sender.sendMessage(mini.deserialize(msg("mapart.claim-limit","<red>You have reached your claim limit.</red>"))); return true; }
                MapArtRegion at = data.regionAt(p.getLocation());
                if (at == null) { sender.sendMessage(mini.deserialize(msg("mapart.claim-not-in-region","<red>Stand inside an unclaimed mapart region.</red>"))); return true; }
                if (data.isClaimed(at.getName())) { sender.sendMessage(mini.deserialize(msg("mapart.claim-already","<red>This mapart region is already claimed.</red>"))); return true; }
                boolean ok = data.claim(at.getName(), p.getUniqueId());
                if (!ok) { sender.sendMessage(mini.deserialize(msg("mapart.claim-failed","<red>Claim failed.</red>"))); return true; }

                // snapshot the plot as-claimed
                snapshotRegionAsync(at);

                // Teleport to the new platform center using helper + auto-set warp
                Location spawn = computePlatformSpawn(at, p);
                if (spawn != null) {
                    data.setWarp(at.getName(), spawn);
                    p.teleportAsync(spawn).thenRun(() ->
                            p.sendMessage(mini.deserialize(
                                    msg("mapart.claim-success","<green>Claimed mapart region:</green>")
                            ).append(mini.deserialize(" <#c1adfe>" + at.getName() + "</#c1adfe>")))
                    );
                } else {
                    // fallback: world missing or null
                    sender.sendMessage(mini.deserialize(
                            msg("mapart.claim-success","<green>Claimed mapart region:</green>")
                    ).append(mini.deserialize(" <#c1adfe>" + at.getName() + "</#c1adfe>")));
                }
                return true;
            }

            case "trust" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.player-only", "<red>Players only.</red>")));
                    return true;
                }
                if (!hasUse(p)) {
                    p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(mini.deserialize(msg("mapart.trust-usage","<gray>/mapart trust <player></gray>")));
                    return true;
                }
                MapArtRegion at = data.regionAt(p.getLocation());
                if (at == null || !data.isClaimed(at.getName())) {
                    sender.sendMessage(mini.deserialize(msg("mapart.not-in-claimed","<red>Stand inside your claimed mapart region.</red>")));
                    return true;
                }
                if (!data.isOwner(at.getName(), p.getUniqueId()) && !hasAdmin(p)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.only-owner","<red>Only the owner can do this.</red>")));
                    return true;
                }

                OfflinePlayer target = getOffline(args[1]);
                if (target == null) {
                    sender.sendMessage(mini.deserialize(msg("mapart.no-such-player","<red>Player not found.</red>")));
                    return true;
                }

                boolean added = data.trust(at.getName(), target.getUniqueId());
                if (!added) {
                    sender.sendMessage(mini.deserialize(msg("mapart.already-trusted","<red>That player is already trusted.</red>")));
                    return true;
                }

                sender.sendMessage(mini.deserialize(
                        msg("mapart.trust-success","<green>Trusted player.</green>")
                ).append(mini.deserialize(" <#c1adfe>" + target.getName() + "</#c1adfe>")));
                return true;
            }

            case "untrust" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }
                if (args.length < 2) { sender.sendMessage(mini.deserialize(msg("mapart.untrust-usage","<gray>/mapart untrust <player></gray>"))); return true; }
                MapArtRegion at = data.regionAt(p.getLocation());
                if (at == null || !data.isClaimed(at.getName())) { sender.sendMessage(mini.deserialize(msg("mapart.not-in-claimed","<red>Stand inside your claimed mapart region.</red>"))); return true; }
                if (!data.isOwner(at.getName(), p.getUniqueId()) && !hasAdmin(p)) { sender.sendMessage(mini.deserialize(msg("mapart.only-owner","<red>Only the owner can do this.</red>"))); return true; }
                OfflinePlayer target = getOffline(args[1]);
                if (target == null) { sender.sendMessage(mini.deserialize(msg("mapart.no-such-player","<red>Player not found.</red>"))); return true; }
                if (!data.untrust(at.getName(), target.getUniqueId())) { sender.sendMessage(mini.deserialize(msg("mapart.not-trusted","<red>That player is not trusted.</red>"))); return true; }
                sender.sendMessage(mini.deserialize(msg("mapart.untrust-success","<green>Removed trust for</green>")).append(mini.deserialize(" <#c1adfe>" + target.getName() + "</#c1adfe>")));
                return true;
            }

            case "lock" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }
                MapArtRegion at = data.regionAt(p.getLocation());
                if (at == null || !data.isClaimed(at.getName())) { sender.sendMessage(mini.deserialize(msg("mapart.not-in-claimed","<red>Stand inside your claimed mapart region.</red>"))); return true; }
                if (!data.isOwner(at.getName(), p.getUniqueId())) { sender.sendMessage(mini.deserialize(msg("mapart.only-owner","<red>Only the owner can do this.</red>"))); return true; }
                boolean locked = data.toggleLock(at.getName());
                sender.sendMessage(mini.deserialize(locked ? msg("mapart.locked","<green>Your mapart is now locked.</green>") : msg("mapart.unlocked","<yellow>Your mapart is now unlocked.</yellow>")));
                return true;
            }

            case "setclaims" -> {
                if (!hasAdmin(sender)) { sender.sendMessage(mini.deserialize(msg("mapart.no-permission","<red>No permission.</red>"))); return true; }
                if (args.length < 3) { sender.sendMessage(mini.deserialize(msg("mapart.setclaims-usage","<gray>/mapart setclaims <player> <count></gray>"))); return true; }
                OfflinePlayer target = getOffline(args[1]);
                if (target == null || !(target.isOnline() || target.hasPlayedBefore())) {
                    sender.sendMessage(mini.deserialize(msg("mapart.no-such-player","<red>Player not found.</red>")));
                    return true;
                }
                int count;
                try { count = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage(mini.deserialize("<red>Count must be a number.</red>")); return true; }
                if (count < 0) count = 0;
                int newVal = data.setBonusClaims(target.getUniqueId(), count);
                String tName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(mini.deserialize("<green>Set bonus claims for </green><#c1adfe>"+tName+"</#c1adfe><green> to </green><#c1adfe>"+newVal+"</#c1adfe><green>.</green>"));
                return true;
            }

            case "setwarp" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>"))); return true; }
                if (!hasUse(p)) { p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>"))); return true; }
                MapArtRegion at = data.regionAt(p.getLocation());
                if (at == null || !data.isClaimed(at.getName())) { p.sendMessage(mini.deserialize("<red>Stand inside your claimed mapart to set a warp.</red>")); return true; }
                if (!data.isOwner(at.getName(), p.getUniqueId()) && !hasAdmin(p)) { p.sendMessage(mini.deserialize(msg("mapart.only-owner","<red>Only the owner can do this.</red>"))); return true; }
                data.setWarp(at.getName(), p.getLocation());
                p.sendMessage(mini.deserialize("<green>Set warp for </green><#c1adfe>"+at.getName()+"</#c1adfe><green>.</green>"));
                return true;
            }

            case "warp" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(mini.deserialize(msg("mapart.player-only","<red>Players only.</red>")));
                    return true;
                }
                if (!hasUse(p)) {
                    p.sendMessage(mini.deserialize(msg("mapart.no-permission-use","<red>You lack permission to use maparts.</red>")));
                    return true;
                }

                // NEW: /mapart warp random
                if (args.length >= 2 && args[1].equalsIgnoreCase("random")) {
                    Map.Entry<String, Location> pick = data.getRandomWarpEntry();
                    if (pick == null || pick.getValue() == null || pick.getValue().getWorld() == null) {
                        p.sendMessage(mini.deserialize(msg("mapart.warp-random-none","<red>No mapart platform warps are available.</red>")));
                        return true;
                    }
                    Location to = pick.getValue();
                    String pickedName = pick.getKey();
                    p.teleportAsync(to).thenRun(() ->
                            p.sendMessage(mini.deserialize(
                                    msg("mapart.warp-random-success","<green>Teleported to a random mapart platform:</green> <#c1adfe>"+pickedName+"</#c1adfe>")
                            ))
                    );
                    return true;
                }

                // Existing: /mapart warp <player> [index]
                if (args.length < 2) {
                    p.sendMessage(mini.deserialize("<gray>/mapart warp <player|random> [index]</gray>"));
                    return true;
                }
                OfflinePlayer target = getOffline(args[1]);
                if (target == null) {
                    p.sendMessage(mini.deserialize(msg("mapart.no-such-player","<red>Player not found.</red>")));
                    return true;
                }

                // Use the list of claimed regions (alphabetical for deterministic order)
                List<String> regions = data.getClaimedRegionNames(target.getUniqueId());
                if (regions.isEmpty()) {
                    p.sendMessage(mini.deserialize("<yellow>That player has no mapart claim.</yellow>"));
                    return true;
                }
                regions.sort(String.CASE_INSENSITIVE_ORDER);

                int idx = 1; // default first
                if (args.length >= 3) {
                    try { idx = Math.max(1, Math.min(Integer.parseInt(args[2]), regions.size())); }
                    catch (NumberFormatException ignored) {}
                }
                String regionName = regions.get(idx - 1);

                // Prefer saved warp; fallback to region center via helper
                Location to = data.getWarp(regionName);
                if (to == null) {
                    MapArtRegion r = null;
                    for (MapArtRegion x : data.getRegions()) {
                        if (x.getName().equalsIgnoreCase(regionName)) { r = x; break; }
                    }
                    to = computeRegionCenter(r);
                }
                if (to == null) {
                    p.sendMessage(mini.deserialize("<red>Warp for that mapart is unavailable.</red>"));
                    return true;
                }

                int finalIdx = idx;
                p.teleportAsync(to).thenRun(() ->
                        p.sendMessage(mini.deserialize("<green>Warped to </green><#c1adfe>"+args[1]+"</#c1adfe><green>'s mapart (#"+finalIdx+").</green>"))
                );
                return true;
            }

            default -> {
                sender.sendMessage(mini.deserialize(msg("mapart.unknown-sub","<red>Unknown subcommand.</red>")));
                return true;
            }
        }
    }

    private boolean hasAdmin(CommandSender s) {
        return s.hasPermission("maparts.admin") || s.hasPermission("mapart.admin");
    }

    private boolean hasUse(CommandSender s) {
        return hasAdmin(s) || s.hasPermission("maparts.use");
    }

    private OfflinePlayer getOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        try { return Bukkit.getOfflinePlayer(name); } catch (Exception e) { return null; }
    }

    private int getMaxClaims(Player p) {
        int best = 0;
        for (String perm : p.getEffectivePermissions().stream().map(ep -> ep.getPermission().toLowerCase(Locale.ROOT)).collect(Collectors.toSet())) {
            if (!perm.startsWith("mapart.claim.")) continue;
            String[] parts = perm.split("\\.");
            if (parts.length != 3) continue;
            try {
                int v = Integer.parseInt(parts[2]);
                if (p.hasPermission("mapart.claim." + v)) best = Math.max(best, v);
            } catch (NumberFormatException ignored) {}
        }
        // include bonus claims
        return best + data.getBonusClaims(p.getUniqueId());
    }

    private MapArtRegion findNearestUnclaimed(Location from) {
        MapArtRegion best = null;
        double bestDist = Double.MAX_VALUE;
        for (MapArtRegion r : data.getRegions()) {
            if (data.isClaimed(r.getName())) continue;
            World w = Bukkit.getWorld(r.getWorldName());
            if (w == null) continue;
            double dist;
            if (!from.getWorld().getName().equalsIgnoreCase(r.getWorldName())) {
                dist = Double.MAX_VALUE / 2; // deprioritize other worlds
            } else {
                Location c = new Location(w, (r.getMinX() + r.getMaxX()) / 2.0 + 0.5, r.getMaxY() + 1.0, (r.getMinZ() + r.getMaxZ()) / 2.0 + 0.5);
                dist = c.distanceSquared(from);
            }
            if (dist < bestDist) { bestDist = dist; best = r; }
        }
        return best;
    }

    private Location computePlatformSpawn(MapArtRegion r, Player refYawFrom) {
        if (r == null) return null;
        World w = Bukkit.getWorld(r.getWorldName());
        if (w == null) return null;

        // Center of platform; stand ON the bedrock at y=65 -> player feet at y=66
        double cx = (r.getMinX() + r.getMaxX()) / 2.0 + 0.5;
        double cz = (r.getMinZ() + r.getMaxZ()) / 2.0 + 0.5;
        double y  = 66.0;

        float yaw = (refYawFrom != null) ? refYawFrom.getLocation().getYaw() : 0f;
        return new Location(w, cx, y, cz, yaw, 0f);
    }

    private Location computeRegionCenter(MapArtRegion r) {
        if (r == null) return null;
        World w = Bukkit.getWorld(r.getWorldName());
        if (w == null) return null;
        return new Location(w, (r.getMinX() + r.getMaxX()) / 2.0 + 0.5, r.getMaxY() + 1.0, (r.getMinZ() + r.getMaxZ()) / 2.0 + 0.5, 0f, 0f);
    }

    private String msg(String key, String def) {
        String s = MessageConfig.get(key);
        return (s == null || s.isEmpty()) ? def : s;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return Arrays.asList("wand","define","claim","trust","untrust","lock","info","trustlist","addclaim","setclaims","setwarp","warp");

        if (args.length == 2 && args[0].equalsIgnoreCase("addclaim") && sender.hasPermission("maparts.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            return Collections.singletonList("auto");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setclaims") && sender.hasPermission("maparts.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setclaims") && sender.hasPermission("maparts.admin")) {
            return Stream.of("0","1","2","3","4","5","6","7","8","9","10")
                    .filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("define")) return Collections.singletonList("<name>");

        // NEW: tab-complete for /mapart warp <player|random>
        if (args.length == 2 && args[0].equalsIgnoreCase("warp")) {
            List<String> options = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
            options.add("random");
            String pre = args[1].toLowerCase(Locale.ROOT);
            return options.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pre)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /* ============================ SNAPSHOT WRITER ============================ */
    private void snapshotRegionAsync(MapArtRegion r) {
        World w = Bukkit.getWorld(r.getWorldName());
        if (w == null) return;

        final int minX = Math.min(r.getMinX(), r.getMaxX());
        final int maxX = Math.max(r.getMinX(), r.getMaxX());
        final int minY = Math.min(r.getMinY(), r.getMaxY());
        final int maxY = Math.max(r.getMinY(), r.getMaxY());
        final int minZ = Math.min(r.getMinZ(), r.getMaxZ());
        final int maxZ = Math.max(r.getMinZ(), r.getMaxZ());

        final String regionName = r.getName();
        final String header =
                "WORLD:" + w.getName() + "\n" +
                        "MINX:" + minX + "\n" +
                        "MAXX:" + maxX + "\n" +
                        "MINY:" + minY + "\n" +
                        "MAXY:" + maxY + "\n" +
                        "MINZ:" + minZ + "\n" +
                        "MAXZ:" + maxZ + "\n" +
                        "FORMAT:RLE-BLOCKDATA-1\n";

        final List<String> rleLines = new ArrayList<>(Math.max(1024, (maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1)/64));

        new BukkitRunnable() {
            int x = minX, y = minY, z = minZ;
            String current = null;
            int run = 0;

            @Override public void run() {
                int ops = 0;
                while (ops < SNAPSHOT_BLOCKS_PER_TICK) {
                    if (y > maxY) {
                        if (run > 0 && current != null) {
                            rleLines.add(run + "|" + current);
                            run = 0;
                            current = null;
                        }
                        cancel();
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshotFile(regionName, header, rleLines));
                        return;
                    }

                    Block b = w.getBlockAt(x, y, z);
                    String dataStr = b.getBlockData().getAsString();
                    if (current == null) { current = dataStr; run = 1; }
                    else if (current.equals(dataStr)) { run++; }
                    else { rleLines.add(run + "|" + current); current = dataStr; run = 1; }

                    ops++;
                    z++;
                    if (z > maxZ) { z = minZ; x++; }
                    if (x > maxX) { x = minX; y++; }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void writeSnapshotFile(String regionName, String header, List<String> rleLines) {
        Path dir = plugin.getDataFolder().toPath().resolve("snapshots");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            plugin.getLogger().warning("[MapArts] Could not create snapshots directory: " + dir + " (" + e.getMessage() + ")");
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
            plugin.getLogger().warning("[MapArts] Snapshot write failed for " + regionName + ": " + ex.getMessage());
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            return;
        }

        try {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            plugin.getLogger().info("[MapArts] Snapshot saved: " + out.getFileName());
        } catch (Exception moveEx) {
            try {
                Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("[MapArts] Snapshot saved (non-atomic move): " + out.getFileName());
            } catch (Exception ex) {
                plugin.getLogger().warning("[MapArts] Snapshot move failed for " + regionName + ": " + ex.getMessage());
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }
    }
}
