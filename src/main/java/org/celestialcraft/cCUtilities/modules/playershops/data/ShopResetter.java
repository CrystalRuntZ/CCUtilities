package org.celestialcraft.cCUtilities.modules.playershops.data;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

public class ShopResetter {
    private final Plugin plugin;
    private final Material floorMaterial;
    private final int blocksPerTick;
    private final boolean clearEntities;

    public ShopResetter(Plugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        String mat = cfg.getString("shops.reset.floor-material", "WHITE_CONCRETE");
        Material m = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
        this.floorMaterial = (m != null) ? m : Material.WHITE_CONCRETE;
        this.blocksPerTick = Math.max(256, cfg.getInt("shops.reset.blocks-per-tick", 4000));
        this.clearEntities = cfg.getBoolean("shops.reset.clear-entities", true);
    }

    /** Preferred: pass the world the player is in as a hint */
    public void reset(ShopRegion region, World worldHint, Runnable onComplete) {
        // try snapshot; if not present or fails, wipe+floor
        if (!restoreFromSnapshot(region, onComplete)) {
            wipeAndFloor(region, worldHint, onComplete);
        }
    }

    /* ---------- snapshot restore (.snap.gz written by ShopsMainCommand) ---------- */
    private boolean restoreFromSnapshot(ShopRegion r, Runnable onComplete) {
        BufferedReader br = null;
        try {
            String rname = r.name();
            File f = new File(plugin.getDataFolder(), "shop_snapshots" + File.separator + rname + ".snap.gz");
            if (!f.exists()) return false;

            br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)), StandardCharsets.UTF_8));

            String worldName = null; int minX=0,maxX=0,minY=0,maxY=0,minZ=0,maxZ=0;
            for (int i = 0; i < 8; i++) {
                String line = br.readLine();
                if (line == null) { br.close(); return false; }
                int idx = line.indexOf(':'); if (idx < 0) continue;
                String k = line.substring(0, idx);
                String v = line.substring(idx + 1);
                switch (k) {
                    case "WORLD" -> worldName = v;
                    case "MINX" -> minX = Integer.parseInt(v);
                    case "MAXX" -> maxX = Integer.parseInt(v);
                    case "MINY" -> minY = Integer.parseInt(v);
                    case "MAXY" -> maxY = Integer.parseInt(v);
                    case "MINZ" -> minZ = Integer.parseInt(v);
                    case "MAXZ" -> maxZ = Integer.parseInt(v);
                }
            }

            if (worldName == null) { br.close(); return false; }
            World w = Bukkit.getWorld(worldName);
            if (w == null) { br.close(); return false; }

            final int fMinX = Math.min(minX, maxX);
            final int fMaxX = Math.max(minX, maxX);
            final int fMinY = Math.min(minY, maxY);
            final int fMaxY = Math.max(minY, maxY);
            final int fMinZ = Math.min(minZ, maxZ);
            final int fMaxZ = Math.max(minZ, maxZ);
            final World fWorld = w;
            final BufferedReader reader = br;

            if (clearEntities) {
                for (Entity e : fWorld.getNearbyEntities(
                        new org.bukkit.util.BoundingBox(fMinX, fMinY, fMinZ, fMaxX + 1, fMaxY + 1, fMaxZ + 1))) {
                    if (!(e instanceof Player)) e.remove();
                }
            }

            final Queue<String> runLines = new ArrayDeque<>();
            for (int i = 0; i < 2048; i++) {
                String ln = reader.readLine();
                if (ln == null) break;
                runLines.add(ln);
            }

            class RLE {
                String currentData = null;
                int remaining = 0;
                BlockData nextData() throws Exception {
                    while (remaining == 0) {
                        if (runLines.isEmpty()) {
                            for (int i = 0; i < 2048; i++) {
                                String ln = reader.readLine();
                                if (ln == null) break;
                                runLines.add(ln);
                            }
                            if (runLines.isEmpty()) return null;
                        }
                        String ln = runLines.remove();
                        int bar = ln.indexOf('|');
                        if (bar <= 0) continue;
                        remaining = Integer.parseInt(ln.substring(0, bar));
                        currentData = ln.substring(bar + 1);
                    }
                    remaining--;
                    return Bukkit.createBlockData(currentData);
                }
            }
            final RLE rle = new RLE();

            new BukkitRunnable() {
                int x = fMinX, y = fMinY, z = fMinZ;
                @Override public void run() {
                    int ops = 0;
                    try {
                        while (ops < blocksPerTick) {
                            if (y > fMaxY) {
                                cancel();
                                try { reader.close(); } catch (Exception ignored) {}
                                if (onComplete != null) onComplete.run();
                                return;
                            }
                            BlockData bd = rle.nextData();
                            if (bd == null) {
                                cancel();
                                try { reader.close(); } catch (Exception ignored) {}
                                if (onComplete != null) onComplete.run();
                                return;
                            }
                            fWorld.getBlockAt(x, y, z).setBlockData(bd, false);
                            ops++;
                            z++;
                            if (z > fMaxZ) { z = fMinZ; x++; }
                            if (x > fMaxX) { x = fMinX; y++; }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("[Shops] Snapshot restore failed for " + rname + ": " + ex.getMessage());
                        cancel();
                        try { reader.close(); } catch (Exception ignored) {}
                        if (onComplete != null) onComplete.run();
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);

            return true;

        } catch (Exception e) {
            if (br != null) try { br.close(); } catch (Exception ignored) {}
            plugin.getLogger().warning("[Shops] Snapshot restore error: " + e.getMessage());
            return false;
        }
    }

    /* ---------- fallback: wipe to air + floor at minY ---------- */
    private void wipeAndFloor(ShopRegion r, World worldHint, Runnable onComplete) {
        World w = (worldHint != null) ? worldHint
                : (r.min() != null ? r.min().getWorld() : null);
        if (w == null) { if (onComplete != null) onComplete.run(); return; }

        final int minX = Math.min(r.min().getBlockX(), r.max().getBlockX());
        final int maxX = Math.max(r.min().getBlockX(), r.max().getBlockX());
        final int minY = Math.min(r.min().getBlockY(), r.max().getBlockY());
        final int maxY = Math.max(r.min().getBlockY(), r.max().getBlockY());
        final int minZ = Math.min(r.min().getBlockZ(), r.max().getBlockZ());
        final int maxZ = Math.max(r.min().getBlockZ(), r.max().getBlockZ());

        if (clearEntities) {
            for (Entity e : w.getNearbyEntities(new org.bukkit.util.BoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1))) {
                if (!(e instanceof Player)) e.remove();
            }
        }

        new BukkitRunnable() {
            int x = minX, y = maxY, z = minZ;
            @Override public void run() {
                int ops = 0;
                while (ops < Math.max(256, blocksPerTick)) {
                    if (y < minY) { cancel(); if (onComplete != null) onComplete.run(); return; }
                    var b = w.getBlockAt(x, y, z);
                    if (y == minY) {
                        if (b.getType() != floorMaterial) b.setType(floorMaterial, false);
                    } else {
                        if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                    }
                    ops++;
                    z++;
                    if (z > maxZ) { z = minZ; x++; }
                    if (x > maxX) { x = minX; y--; }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
