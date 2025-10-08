package org.celestialcraft.cCUtilities.modules.maparts;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MapArtDataManager {
    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final List<MapArtRegion> regions = new ArrayList<>();
    private final Map<String, UUID> claimedBy = new HashMap<>();
    private final Map<String, Set<UUID>> trusted = new HashMap<>();
    private final Set<String> locked = new HashSet<>();

    // NEW: bonus claims per player
    private final Map<UUID, Integer> bonusClaims = new HashMap<>();
    // NEW: per-region warp locations
    private final Map<String, Location> warps = new HashMap<>();
    private final Random rng = new Random();

    public MapArtDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maparts.yml");
        reload();
    }

    public synchronized void reload() {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    plugin.getLogger().warning("[MapArts] Failed to create data folder: " + parent.getAbsolutePath());
                }
            }
            try {
                if (!file.exists() && !file.createNewFile()) {
                    plugin.getLogger().warning("[MapArts] Failed to create data file: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[MapArts] Exception creating data file: " + e.getMessage());
            }
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        regions.clear();
        claimedBy.clear();
        trusted.clear();
        locked.clear();
        bonusClaims.clear();
        warps.clear();

        ConfigurationSection r = yaml.getConfigurationSection("regions");
        if (r != null) {
            for (String key : r.getKeys(false)) {
                ConfigurationSection c = r.getConfigurationSection(key);
                if (c == null) continue;
                String world = c.getString("world");
                int minX = c.getInt("minX"), minY = c.getInt("minY"), minZ = c.getInt("minZ");
                int maxX = c.getInt("maxX"), maxY = c.getInt("maxY"), maxZ = c.getInt("maxZ");
                regions.add(new MapArtRegion(key, world, minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        ConfigurationSection cl = yaml.getConfigurationSection("claims");
        if (cl != null) {
            for (String regionName : cl.getKeys(false)) {
                String uuidStr = cl.getString(regionName);
                try {
                    if (uuidStr != null) claimedBy.put(regionName, UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        ConfigurationSection tr = yaml.getConfigurationSection("trusted");
        if (tr != null) {
            for (String regionName : tr.getKeys(false)) {
                List<String> list = tr.getStringList(regionName);
                Set<UUID> set = new HashSet<>();
                for (String s : list) {
                    try { set.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                if (!set.isEmpty()) trusted.put(regionName, set);
            }
        }

        ConfigurationSection lk = yaml.getConfigurationSection("locked");
        if (lk != null) {
            for (String regionName : lk.getKeys(false)) {
                if (lk.getBoolean(regionName, false)) locked.add(regionName);
            }
        }

        // NEW: bonus claims
        ConfigurationSection bc = yaml.getConfigurationSection("bonusClaims");
        if (bc != null) {
            for (String key : bc.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    int v = bc.getInt(key, 0);
                    if (v > 0) bonusClaims.put(id, v);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // NEW: warps (Bukkit Location is ConfigurationSerializable)
        ConfigurationSection ws = yaml.getConfigurationSection("warps");
        if (ws != null) {
            for (String regionName : ws.getKeys(false)) {
                Location loc = yaml.getLocation("warps." + regionName);
                if (loc != null) warps.put(regionName, loc);
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (MapArtRegion reg : regions) {
            String path = "regions." + reg.getName();
            out.set(path + ".world", reg.getWorldName());
            out.set(path + ".minX", reg.getMinX());
            out.set(path + ".minY", reg.getMinY());
            out.set(path + ".minZ", reg.getMinZ());
            out.set(path + ".maxX", reg.getMaxX());
            out.set(path + ".maxY", reg.getMaxY());
            out.set(path + ".maxZ", reg.getMaxZ());
        }
        for (Map.Entry<String, UUID> e : claimedBy.entrySet()) {
            out.set("claims." + e.getKey(), e.getValue().toString());
        }
        for (Map.Entry<String, Set<UUID>> e : trusted.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (UUID u : e.getValue()) ids.add(u.toString());
            out.set("trusted." + e.getKey(), ids);
        }
        for (String name : locked) {
            out.set("locked." + name, true);
        }
        // NEW: bonus claims
        for (Map.Entry<UUID, Integer> e : bonusClaims.entrySet()) {
            if (e.getValue() > 0) out.set("bonusClaims." + e.getKey(), e.getValue());
        }
        // NEW: warps
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            out.set("warps." + e.getKey(), e.getValue());
        }

        try {
            out.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("[MapArts] Save failed: " + ex.getMessage());
        }
        yaml = out;
    }

    public synchronized boolean regionExists(String name) {
        return regions.stream().anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }

    public synchronized void addRegion(MapArtRegion region) {
        regions.add(region);
        save();
    }

    public synchronized MapArtRegion regionAt(org.bukkit.Location loc) {
        for (MapArtRegion r : regions) if (r.contains(loc)) return r;
        return null;
    }

    public synchronized boolean isClaimed(String regionName) {
        return claimedBy.containsKey(regionName);
    }

    public synchronized UUID getClaimer(String regionName) {
        return claimedBy.get(regionName);
    }

    public synchronized boolean claim(String regionName, UUID uuid) {
        if (claimedBy.containsKey(regionName)) return false;
        claimedBy.put(regionName, uuid);
        save();
        return true;
    }

    public synchronized int countClaims(UUID uuid) {
        int c = 0;
        for (UUID u : claimedBy.values()) if (uuid.equals(u)) c++;
        return c;
    }

    public synchronized boolean isOwner(String regionName, UUID uuid) {
        UUID o = claimedBy.get(regionName);
        return o != null && o.equals(uuid);
    }

    public synchronized boolean isTrusted(String regionName, UUID uuid) {
        Set<UUID> set = trusted.get(regionName);
        return set != null && set.contains(uuid);
    }

    public synchronized boolean trust(String regionName, UUID uuid) {
        Set<UUID> set = trusted.computeIfAbsent(regionName, k -> new HashSet<>());
        boolean added = set.add(uuid);
        if (added) save();
        return added;
    }

    public synchronized boolean untrust(String regionName, UUID uuid) {
        Set<UUID> set = trusted.get(regionName);
        if (set == null) return false;
        boolean removed = set.remove(uuid);
        if (removed) save();
        return removed;
    }

    public synchronized boolean isLocked(String regionName) {
        return locked.contains(regionName);
    }

    public synchronized boolean toggleLock(String regionName) {
        boolean nowLocked;
        if (locked.contains(regionName)) {
            locked.remove(regionName);
            nowLocked = false;
        } else {
            locked.add(regionName);
            nowLocked = true;
        }
        save();
        return nowLocked;
    }

    // expose trusted set copy
    public synchronized Set<UUID> getTrusted(String regionName) {
        return new HashSet<>(trusted.getOrDefault(regionName, Collections.emptySet()));
    }

    // bonus claims helpers
    public synchronized int getBonusClaims(UUID uuid) {
        return bonusClaims.getOrDefault(uuid, 0);
    }

    public synchronized int addBonusClaims(UUID uuid, int delta) {
        int cur = bonusClaims.getOrDefault(uuid, 0) + delta;
        if (cur <= 0) {
            bonusClaims.remove(uuid);
            cur = 0;
        } else {
            bonusClaims.put(uuid, cur);
        }
        save();
        return cur;
    }

    public synchronized int addOneBonusClaim(UUID uuid) {
        return addBonusClaims(uuid, 1);
    }

    public synchronized int setBonusClaims(UUID uuid, int count) {
        if (count <= 0) {
            bonusClaims.remove(uuid);
            count = 0;
        } else {
            bonusClaims.put(uuid, count);
        }
        save();
        return count;
    }

    // warps (per-region)
    public synchronized void setWarp(String regionName, Location loc) {
        warps.put(regionName, loc);
        save();
    }

    public synchronized Location getWarp(String regionName) {
        return warps.get(regionName);
    }

    // NEW: random platform warp helpers
    public synchronized Map.Entry<String, Location> getRandomWarpEntry() {
        if (warps.isEmpty()) return null;
        // filter to valid worlds
        List<Map.Entry<String, Location>> valid = new ArrayList<>();
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            Location loc = e.getValue();
            if (loc != null && loc.getWorld() != null) valid.add(e);
        }
        if (valid.isEmpty()) return null;
        return valid.get(rng.nextInt(valid.size()));
    }

    // claimed regions lookup
    public synchronized List<String> getClaimedRegionNames(UUID owner) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, UUID> e : claimedBy.entrySet()) {
            if (owner.equals(e.getValue())) list.add(e.getKey());
        }
        return list;
    }

    public synchronized List<MapArtRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /* ===================== Auto-name helper for /mapart define ===================== */

    private static String sanitizeWorldName(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    /** Returns the next available auto-name for a given world, e.g., "mapart-world-001". */
    public synchronized String nextAutoName(String worldName) {
        String prefix = "mapart-" + sanitizeWorldName(worldName) + "-";
        int max = 0;
        for (MapArtRegion r : regions) {
            String n = r.getName();
            String lower = n.toLowerCase(Locale.ROOT);
            if (lower.startsWith(prefix)) {
                String tail = n.substring(prefix.length());
                try {
                    int v = Integer.parseInt(tail);
                    if (v > max) max = v;
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix + String.format("%03d", max + 1);
    }
}
