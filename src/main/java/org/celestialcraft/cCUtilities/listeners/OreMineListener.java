package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.orewatcher.NotificationTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

/**
 * Notifies staff when a player breaks an ore vein.
 * Fixes:
 *  - Reserve per-player cooldown immediately to prevent duplicate notifications.
 *  - Bound flood-fill by radius from the START block (using squared distance).
 *  - Ignore cancelled block breaks.
 *  - Add a GLOBAL throttle: at most one broadcast every 3 seconds, regardless of player.
 */
public class OreMineListener implements Listener {

    private final NotificationTracker tracked = new NotificationTracker();
    private final Set<Material> watchBlocks;
    private final int radius;
    private final long cooldown;
    private final String permission;

    // Global throttle (ms). Only one broadcast every 3 seconds regardless of player.
    private long nextGlobalBroadcastAt = 0L;

    public OreMineListener(FileConfiguration config) {
        this.watchBlocks = new HashSet<>();
        for (String mat : config.getStringList("orewatcher.blocks")) {
            Material material = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
            if (material != null) watchBlocks.add(material);
        }
        this.radius = config.getInt("orewatcher.radius", 8);
        this.cooldown = config.getLong("orewatcher.cooldown-seconds", 5) * 1000L;
        this.permission = config.getString("orewatcher.notify-permission", "OreWatcher.notify");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("orewatcher")) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!watchBlocks.contains(block.getType())) return;

        // ---- GLOBAL THROTTLE (3s between ANY broadcasts) ----
        long now = System.currentTimeMillis();
        if (now < nextGlobalBroadcastAt) return;
        // Tentatively claim the window; if we exit early below, that's fine—we just skip a burst-y duplicate.
        nextGlobalBroadcastAt = now + 3000L;

        // ---- Per-player cooldown: reserve immediately to prevent duplicates for the same player ----
        UUID uid = player.getUniqueId();
        if (tracked.isOnCooldown(uid)) return;
        tracked.setCooldown(uid, cooldown);

        // Find connected vein blocks, bounded by radius from the START block
        Set<Block> connected = findConnected(block, new HashSet<>(), radius);

        String raw = MessageConfig.get("orewatcher.message")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(connected.size()))
                .replace("%block%", block.getType().name())
                .replace("%x%", String.valueOf(block.getX()))
                .replace("%y%", String.valueOf(block.getY()))
                .replace("%z%", String.valueOf(block.getZ()));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) {
                p.sendMessage(MiniMessage.miniMessage().deserialize(raw));
            }
        }
    }

    /**
     * Flood-fill same-type blocks connected in 26 directions, bounded by radius from the START block.
     */
    private Set<Block> findConnected(Block start, Set<Block> visited, int radius) {
        final int r2 = radius * radius;
        final int sx = start.getX();
        final int sy = start.getY();
        final int sz = start.getZ();
        final Material type = start.getType();

        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Block nearby = current.getRelative(dx, dy, dz);
                        if (visited.contains(nearby)) continue;

                        // Bound by distance from the START block, not the current node
                        int nx = nearby.getX();
                        int ny = nearby.getY();
                        int nz = nearby.getZ();
                        int dist2 = (nx - sx) * (nx - sx) + (ny - sy) * (ny - sy) + (nz - sz) * (nz - sz);
                        if (dist2 > r2) continue;

                        if (nearby.getType() != type) continue;

                        visited.add(nearby);
                        queue.add(nearby);
                    }
                }
            }
        }

        return visited;
    }
}
