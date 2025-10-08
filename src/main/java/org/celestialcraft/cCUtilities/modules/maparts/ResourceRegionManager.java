package org.celestialcraft.cCUtilities.modules.maparts;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ResourceRegionManager {
    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    private final Map<String, ResourceRegion> regions = new HashMap<>();

    private static final int FILL_BLOCKS_PER_TICK = 8000;
    private static final int SCAN_BLOCKS_PER_TICK = 16000;
    private static final double RESET_AIR_RATIO = 0.70;

    // scan throttling
    private final Map<String, Long> lastScanAtMs = new HashMap<>();
    private static final long SCAN_THROTTLE_MS = 2500;

    public ResourceRegionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "resource-regions.yml");
        reload();
    }

    public synchronized void reload() {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    boolean made = parent.mkdirs();
                    if (!made && !parent.exists()) {
                        plugin.getLogger().warning("[MapArts] Failed to create data folder: " + parent.getAbsolutePath());
                    }
                }
                if (!file.exists()) {
                    boolean created = file.createNewFile();
                    if (!created && !file.exists()) {
                        plugin.getLogger().warning("[MapArts] Failed to create data file: " + file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[MapArts] Exception creating data file: " + e.getMessage());
            }
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        regions.clear(); // <-- only this belongs here

        ConfigurationSection sec = yaml.getConfigurationSection("regions");
        if (sec != null) {
            for (String name : sec.getKeys(false)) {
                ConfigurationSection r = sec.getConfigurationSection(name);
                if (r == null) continue;
                String world = r.getString("world");
                int minX = r.getInt("minX"), minY = r.getInt("minY"), minZ = r.getInt("minZ");
                int maxX = r.getInt("maxX"), maxY = r.getInt("maxY"), maxZ = r.getInt("maxZ");
                String fill = r.getString("fill", Material.RED_CONCRETE.name());
                Material mat = Material.matchMaterial(fill);
                if (world != null && mat != null) {
                    regions.put(name, new ResourceRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ, mat));
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (ResourceRegion rr : regions.values()) {
            String p = "regions." + rr.getName();
            out.set(p + ".world", rr.getWorldName());
            out.set(p + ".minX", rr.getMinX());
            out.set(p + ".minY", rr.getMinY());
            out.set(p + ".minZ", rr.getMinZ());
            out.set(p + ".maxX", rr.getMaxX());
            out.set(p + ".maxY", rr.getMaxY());
            out.set(p + ".maxZ", rr.getMaxZ());
            out.set(p + ".fill", rr.getFillMaterial().name());
        }
        try { out.save(file); } catch (IOException ignored) {}
        yaml = out;
    }

    public synchronized boolean exists(String name) { return regions.containsKey(name); }
    public synchronized ResourceRegion get(String name) { return regions.get(name); }
    public synchronized Collection<ResourceRegion> all() { return Collections.unmodifiableCollection(regions.values()); }

    public synchronized void add(ResourceRegion rr) { regions.put(rr.getName(), rr); save(); }
    public synchronized boolean remove(String name) { boolean ok = regions.remove(name) != null; if (ok) save(); return ok; }

    public ResourceRegion regionAt(Location loc) {
        if (loc == null) return null;
        for (ResourceRegion rr : all()) if (rr.contains(loc)) return rr;
        return null;
    }

    /* ======================== WORLD EDITING HELPERS ======================== */

    /** Fill region to its fill material, spread across ticks to avoid lag. */
    public void fillRegion(ResourceRegion rr) {
        World w = rr.world();
        if (w == null) return;

        final int minX = rr.getMinX(), minY = rr.getMinY(), minZ = rr.getMinZ();
        final int maxX = rr.getMaxX(), maxY = rr.getMaxY(), maxZ = rr.getMaxZ();
        final Material fill = rr.getFillMaterial();

        new BukkitRunnable() {
            int x = minX, y = minY, z = minZ;
            @Override public void run() {
                int ops = 0;
                while (ops < FILL_BLOCKS_PER_TICK) {
                    if (y > maxY) { cancel(); return; }
                    w.getBlockAt(x, y, z).setType(fill, false);
                    ops++;
                    z++; if (z > maxZ) { z = minZ; x++; }
                    if (x > maxX) { x = minX; y++; }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Scan region air ratio. If >= RESET_AIR_RATIO then triggers fill. */
    public void scanAndMaybeReset(ResourceRegion rr) {
        long now = System.currentTimeMillis();
        Long last = lastScanAtMs.get(rr.getName());
        if (last != null && now - last < SCAN_THROTTLE_MS) return; // throttle
        lastScanAtMs.put(rr.getName(), now);

        World w = rr.world();
        if (w == null) return;

        final int minX = rr.getMinX(), minY = rr.getMinY(), minZ = rr.getMinZ();
        final int maxX = rr.getMaxX(), maxY = rr.getMaxY(), maxZ = rr.getMaxZ();

        final long total = rr.volume();
        if (total <= 0) return;

        final long[] airCount = {0};
        final int[] x = {minX}, y = {minY}, z = {minZ};

        new BukkitRunnable() {
            @Override public void run() {
                int ops = 0;
                while (ops < SCAN_BLOCKS_PER_TICK) {
                    if (y[0] > maxY) {
                        cancel();
                        double ratio = airCount[0] / (double) total;
                        if (ratio >= RESET_AIR_RATIO) fillRegion(rr);
                        return;
                    }
                    if (w.getBlockAt(x[0], y[0], z[0]).isEmpty()) airCount[0]++;

                    ops++;
                    z[0]++; if (z[0] > maxZ) { z[0] = minZ; x[0]++; }
                    if (x[0] > maxX) { x[0] = minX; y[0]++; }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
