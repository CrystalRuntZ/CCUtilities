package org.celestialcraft.cCUtilities.modules.playershops.data;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopDataManager {
    private static final List<ShopRegion> shopRegions = new ArrayList<>();
    private static final Map<UUID, String> claimedBy = new HashMap<>();
    private static final Map<String, Location> warps = new HashMap<>();
    private static final Map<String, Set<UUID>> trustedPlayers = new HashMap<>();
    private static final Map<String, Long> lastUpdatedMap = new HashMap<>();
    private static final Map<String, String> regionWorldNames = new HashMap<>();

    private static File file;
    private static YamlConfiguration config;
    private static JavaPlugin plugin;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static boolean saveScheduled = false;
    private static void scheduleSave() {
        if (saveScheduled || plugin == null) return;
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            save();
        }, 40L);
    }

    public static void load(JavaPlugin plugin) {
        ShopDataManager.plugin = plugin;
        file = new File(plugin.getDataFolder(), "shops.yml");

        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("[PlayerShops] Failed to create data folder: " + parent.getAbsolutePath());
            }
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("[PlayerShops] shops.yml already existed or could not be created: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[PlayerShops] Failed to create shops.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        shopRegions.clear();
        claimedBy.clear();
        warps.clear();
        trustedPlayers.clear();
        lastUpdatedMap.clear();
        regionWorldNames.clear();

        if (config.isConfigurationSection("shops")) {
            for (String name : Objects.requireNonNull(config.getConfigurationSection("shops")).getKeys(false)) {
                String base = "shops." + name;
                String worldName = config.getString(base + ".world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world == null) world = Bukkit.getWorlds().getFirst();

                var minV = config.getVector(base + ".min");
                var maxV = config.getVector(base + ".max");
                if (minV == null || maxV == null) continue;

                boolean divine = config.getBoolean(base + ".divine",
                        name != null && name.toLowerCase(Locale.ROOT).startsWith("divine"));

                Location min = minV.toLocation(world);
                Location max = maxV.toLocation(world);
                shopRegions.add(new ShopRegion(name, min, max, divine));
                if (worldName != null) regionWorldNames.put(name, worldName);

                long last = config.getLong(base + ".last-updated", 0L);
                if (last > 0) lastUpdatedMap.put(name, last);
            }
        }

        if (config.isConfigurationSection("claims")) {
            for (String uuid : Objects.requireNonNull(config.getConfigurationSection("claims")).getKeys(false)) {
                String shop = config.getString("claims." + uuid);
                if (shop != null) claimedBy.put(UUID.fromString(uuid), shop);
            }
        }

        if (config.isConfigurationSection("warps")) {
            for (String shop : Objects.requireNonNull(config.getConfigurationSection("warps")).getKeys(false)) {
                String path = "warps." + shop;
                String worldName = config.getString(path + ".world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world == null) continue;
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw");
                float pitch = (float) config.getDouble(path + ".pitch");
                warps.put(shop, new Location(world, x, y, z, yaw, pitch));
            }
        }

        if (config.isConfigurationSection("trusted")) {
            for (String shop : Objects.requireNonNull(config.getConfigurationSection("trusted")).getKeys(false)) {
                List<String> list = config.getStringList("trusted." + shop);
                Set<UUID> set = new HashSet<>();
                for (String s : list) {
                    try { set.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                trustedPlayers.put(shop, set);
            }
        }
    }

    public static void save() {
        config.set("shops", null);
        for (ShopRegion region : shopRegions) {
            String base = "shops." + region.name();
            String worldName = regionWorldNames.getOrDefault(region.name(),
                    region.min().getWorld() != null ? region.min().getWorld().getName() : Bukkit.getWorlds().getFirst().getName());
            config.set(base + ".world", worldName);
            config.set(base + ".min", region.min().toVector());
            config.set(base + ".max", region.max().toVector());
            config.set(base + ".divine", region.divine());
            if (lastUpdatedMap.containsKey(region.name())) {
                config.set(base + ".last-updated", lastUpdatedMap.get(region.name()));
            }
        }

        config.set("claims", null);
        for (Map.Entry<UUID, String> e : claimedBy.entrySet()) {
            config.set("claims." + e.getKey(), e.getValue());
        }

        config.set("warps", null);
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            String base = "warps." + e.getKey();
            Location l = e.getValue();
            config.set(base + ".world", l.getWorld().getName());
            config.set(base + ".x", l.getX());
            config.set(base + ".y", l.getY());
            config.set(base + ".z", l.getZ());
            config.set(base + ".yaw", l.getYaw());
            config.set(base + ".pitch", l.getPitch());
        }

        config.set("trusted", null);
        for (Map.Entry<String, Set<UUID>> e : trustedPlayers.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID u : e.getValue()) list.add(u.toString());
            config.set("trusted." + e.getKey(), list);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[PlayerShops] Failed to save shops.yml: " + e.getMessage());
        }
    }

    public static void addShop(ShopRegion region) {
        shopRegions.add(region);
        if (region.min() != null && region.min().getWorld() != null) {
            regionWorldNames.put(region.name(), region.min().getWorld().getName());
        }
        scheduleSave();
    }

    public static boolean isOverlapping(ShopRegion n) {
        return shopRegions.stream().anyMatch(e -> e.overlaps(n));
    }

    public static ShopRegion getRegionAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        String lw = loc.getWorld().getName();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        for (ShopRegion r : shopRegions) {
            String rw = regionWorldNames.getOrDefault(
                    r.name(),
                    r.min() != null && r.min().getWorld() != null ? r.min().getWorld().getName() : null
            );
            if (rw == null || !rw.equalsIgnoreCase(lw)) continue;

            assert r.min() != null;
            int minX = Math.min(r.min().getBlockX(), r.max().getBlockX());
            int maxX = Math.max(r.min().getBlockX(), r.max().getBlockX());
            int minZ = Math.min(r.min().getBlockZ(), r.max().getBlockZ());
            int maxZ = Math.max(r.min().getBlockZ(), r.max().getBlockZ());

            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return r; // ignore Y: treat as full-height column
            }
        }
        return null;
    }

    public static boolean isClaimed(String name) {
        return claimedBy.containsValue(name);
    }

    public static boolean hasClaimed(UUID player) {
        return claimedBy.containsKey(player);
    }

    public static String getClaim(UUID player) {
        return claimedBy.get(player);
    }

    public static UUID getOwnerUUID(String shopName) {
        for (Map.Entry<UUID, String> e : claimedBy.entrySet()) {
            if (e.getValue().equalsIgnoreCase(shopName)) return e.getKey();
        }
        return null;
    }

    public static void claimShop(UUID player, String name) {
        claimedBy.put(player, name);
        scheduleSave();
    }

    public static void setWarp(String shop, Location location) {
        warps.put(shop, location);
        scheduleSave();
    }

    public static Location getWarp(String shop) {
        return warps.get(shop);
    }

    public static Location getRandomWarpActive(int days) {
        if (warps.isEmpty()) return null;
        long cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L;
        List<String> eligible = new ArrayList<>();
        for (String shop : warps.keySet()) {
            Long last = lastUpdatedMap.get(shop);
            if (last != null && last >= cutoff) {
                eligible.add(shop);
            }
        }
        if (eligible.isEmpty()) return null;
        String pick = eligible.get(new Random().nextInt(eligible.size()));
        return warps.get(pick);
    }

    public static boolean unclaimShopAt(Location location) {
        ShopRegion region = getRegionAt(location);
        if (region == null) return false;
        UUID owner = getOwnerUUID(region.name());
        if (owner == null) return false;

        claimedBy.remove(owner);
        warps.remove(region.name());
        trustedPlayers.remove(region.name());
        scheduleSave();
        return true;
    }

    public static void addTrusted(String shop, UUID uuid) {
        trustedPlayers.computeIfAbsent(shop, k -> new HashSet<>()).add(uuid);
        scheduleSave();
    }

    public static Set<UUID> getTrusted(String shop) {
        return trustedPlayers.getOrDefault(shop, Collections.emptySet());
    }

    public static void removeTrusted(String shop, UUID uuid) {
        Set<UUID> t = trustedPlayers.get(shop);
        if (t != null && t.remove(uuid)) scheduleSave();
    }

    public static boolean isTrusted(String shop, UUID uuid) {
        return trustedPlayers.getOrDefault(shop, Collections.emptySet()).contains(uuid);
    }

    public static boolean isDivine(ShopRegion shop) {
        return shop != null && shop.divine();
    }

    public static void setLastUpdated(String shop, long time) {
        lastUpdatedMap.put(shop, time);
        scheduleSave();
    }

    public static Long getLastUpdated(String shop) {
        return lastUpdatedMap.get(shop);
    }

    private static boolean hasRegionName(String name) {
        for (ShopRegion r : shopRegions) {
            if (r.name().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static String nextRegionName(boolean divine) {
        String prefix = divine ? "divine" : "plot";
        int n = 1;
        while (hasRegionName(prefix + n)) n++;
        return prefix + n;
    }

    /** ---------- NEW: next region name by type (plot/divine/jupiter/ascendant) ---------- */
    private static String nextRegionNameForType(String type) {
        String prefix;
        if (type == null) {
            prefix = "plot";
        } else {
            switch (type.toLowerCase(Locale.ROOT)) {
                case "divine" -> prefix = "divine";
                case "saturn" -> prefix = "saturn";       // <--- was "jupiter"
                case "ascendant" -> prefix = "ascendant";
                default -> prefix = "plot";
            }
        }
        int n = 1;
        while (hasRegionName(prefix + n)) n++;
        return prefix + n;
    }

    /** Keeps existing boolean path; "divine" sets divine=true; others are name-prefixed only. */
    public static void defineShopRegionType(Player player, String type) {
        boolean divine = "divine".equalsIgnoreCase(type);
        String name = nextRegionNameForType(type);

        ShopRegion newRegion = buildRegionFromSelection(player.getUniqueId(), name, divine);
        if (newRegion == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-define-missing")));
            return;
        }
        if (isOverlapping(newRegion)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-define-overlap")));
            return;
        }
        addShop(newRegion);
        if (newRegion.min() != null && newRegion.min().getWorld() != null) {
            regionWorldNames.put(newRegion.name(), newRegion.min().getWorld().getName());
        }
        assert newRegion.min() != null;
        player.sendMessage(mm.deserialize(
                MessageConfig.get("playershops.region-define-success")
                        .replace("%name%", newRegion.name())
                        .replace("%x1%", String.valueOf(newRegion.min().getBlockX()))
                        .replace("%y1%", String.valueOf(newRegion.min().getBlockY()))
                        .replace("%z1%", String.valueOf(newRegion.min().getBlockZ()))
                        .replace("%x2%", String.valueOf(newRegion.max().getBlockX()))
                        .replace("%y2%", String.valueOf(newRegion.max().getBlockY()))
                        .replace("%z2%", String.valueOf(newRegion.max().getBlockZ()))
        ));
    }

    public static void defineShopRegion(Player player, boolean divine) {
        ShopRegion newRegion = buildRegionFromSelection(player.getUniqueId(), nextRegionName(divine), divine);
        if (newRegion == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-define-missing")));
            return;
        }
        if (isOverlapping(newRegion)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.region-define-overlap")));
            return;
        }
        addShop(newRegion);
        regionWorldNames.put(newRegion.name(), newRegion.min().getWorld().getName());
        player.sendMessage(mm.deserialize(
                MessageConfig.get("playershops.region-define-success")
                        .replace("%name%", newRegion.name())
                        .replace("%x1%", String.valueOf(newRegion.min().getBlockX()))
                        .replace("%y1%", String.valueOf(newRegion.min().getBlockY()))
                        .replace("%z1%", String.valueOf(newRegion.min().getBlockZ()))
                        .replace("%x2%", String.valueOf(newRegion.max().getBlockX()))
                        .replace("%y2%", String.valueOf(newRegion.max().getBlockY()))
                        .replace("%z2%", String.valueOf(newRegion.max().getBlockZ()))
        ));
    }

    // In ShopDataManager.buildRegionFromSelection(...)
    private static ShopRegion buildRegionFromSelection(UUID uuid, String name, boolean divine) {
        Location pos1 = ShopSelectionStorage.getPos1(uuid);
        Location pos2 = ShopSelectionStorage.getPos2(uuid);
        if (pos1 == null || pos2 == null) return null;
        if (!Objects.equals(pos1.getWorld(), pos2.getWorld())) return null;

        World w = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Make plots full-height columns
        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight() - 1;

        Location min = new Location(w, minX, minY, minZ);
        Location max = new Location(w, maxX, maxY, maxZ);

        return new ShopRegion(name, min, max, divine);
    }

    public static boolean deleteRegion(String name) {
        if (name == null) return false;

        ShopRegion target = null;
        for (ShopRegion r : shopRegions) {
            if (r.name().equalsIgnoreCase(name)) {
                target = r;
                break;
            }
        }
        if (target == null) return false;

        claimedBy.entrySet().removeIf(e -> e.getValue() != null && e.getValue().equalsIgnoreCase(name));
        removeKeyIgnoreCase(warps, name);
        removeKeyIgnoreCase(trustedPlayers, name);
        removeKeyIgnoreCase(lastUpdatedMap, name);
        removeKeyIgnoreCase(regionWorldNames, name);

        shopRegions.remove(target);

        scheduleSave();
        return true;
    }

    private static <T> void removeKeyIgnoreCase(Map<String, T> map, String key) {
        if (map == null || map.isEmpty() || key == null) return;
        Iterator<Map.Entry<String, T>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, T> e = it.next();
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                it.remove();
                break;
            }
        }
    }
}
