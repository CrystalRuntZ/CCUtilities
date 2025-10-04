package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.orewatcher.NotificationTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OreMineListener implements Listener {

    // ---- cross-instance guards ----
    private static final NotificationTracker TRACKED = new NotificationTracker();
    private static volatile long NEXT_GLOBAL_BROADCAST_AT = 0L;
    private static final long GLOBAL_THROTTLE_MS = 3000L;

    // short-lived de-dupe of the exact block (handles double-registration within same tick)
    private static final Map<String, Long> RECENT_KEYS = new ConcurrentHashMap<>();
    private static final long DEDUPE_WINDOW_MS = 250L;

    // ---- config-derived per-instance data ----
    private final Set<Material> watchBlocks;
    private final int radius;
    private final long cooldownMs;
    private final String permission;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public OreMineListener(FileConfiguration config) {
        this.watchBlocks = new HashSet<>();
        for (String mat : config.getStringList("orewatcher.blocks")) {
            Material material = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
            if (material != null) watchBlocks.add(material);
        }
        this.radius = config.getInt("orewatcher.radius", 8);
        this.cooldownMs = config.getLong("orewatcher.cooldown-seconds", 5) * 1000L;
        this.permission = config.getString("orewatcher.notify-permission", "OreWatcher.notify");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("orewatcher")) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        if (!watchBlocks.contains(type)) return;

        // ---- tiny per-block de-duplication (handles accidental double registration) ----
        long now = System.currentTimeMillis();
        String key = block.getWorld().getUID() + "#" + block.getX() + "," + block.getY() + "," + block.getZ();
        Long last = RECENT_KEYS.put(key, now);
        if (last != null && (now - last) <= DEDUPE_WINDOW_MS) return;
        // opportunistic cleanup
        if (RECENT_KEYS.size() > 4096) {
            long cutoff = now - DEDUPE_WINDOW_MS;
            RECENT_KEYS.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        // ---- GLOBAL THROTTLE ----
        long nextAt = NEXT_GLOBAL_BROADCAST_AT;
        if (now < nextAt) return;
        NEXT_GLOBAL_BROADCAST_AT = now + GLOBAL_THROTTLE_MS;

        // ---- Per-player cooldown (shared across all instances) ----
        UUID uid = player.getUniqueId();
        if (TRACKED.isOnCooldown(uid)) return;
        TRACKED.setCooldown(uid, cooldownMs);

        // ---- Find connected vein (bounded by radius from the START block) ----
        Set<Block> connected = findConnected(block, new HashSet<>(), radius);

        String raw = MessageConfig.get("orewatcher.message")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(connected.size()))
                .replace("%block%", type.name())
                .replace("%x%", String.valueOf(block.getX()))
                .replace("%y%", String.valueOf(block.getY()))
                .replace("%z%", String.valueOf(block.getZ()));

        var msg = MM.deserialize(raw);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) {
                p.sendMessage(msg);
            }
        }
    }

    /**
     * Flood-fill same-type blocks connected in 26 directions, bounded by radius from the START block.
     */
    private Set<Block> findConnected(Block start, Set<Block> visited, int radius) {
        final int r2 = radius * radius;
        final int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        final Material type = start.getType();

        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Block nb = current.getRelative(dx, dy, dz);
                        if (visited.contains(nb)) continue;

                        int nx = nb.getX(), ny = nb.getY(), nz = nb.getZ();
                        int dist2 = (nx - sx) * (nx - sx) + (ny - sy) * (ny - sy) + (nz - sz) * (nz - sz);
                        if (dist2 > r2) continue;

                        if (nb.getType() != type) continue;

                        visited.add(nb);
                        queue.add(nb);
                    }
                }
            }
        }
        return visited;
    }
}
