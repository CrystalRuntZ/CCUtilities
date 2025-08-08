package org.celestialcraft.cCUtilities.modules.playershops.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopDataManager {
    private static final List<ShopRegion> shopRegions = new ArrayList<>();
    private static final Map<UUID, String> claimedBy = new HashMap<>();
    private static final Map<String, Location> warps = new HashMap<>();
    private static final Map<String, Set<UUID>> trustedPlayers = new HashMap<>();
    private static final Map<String, Long> lastUpdatedMap = new HashMap<>();

    private static File file;
    private static YamlConfiguration config;
    private static JavaPlugin plugin;

    public static void load(JavaPlugin plugin) {
        ShopDataManager.plugin = plugin;
        file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Failed to create shops.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("IOException while creating shops.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.isConfigurationSection("shops")) {
            for (String name : Objects.requireNonNull(config.getConfigurationSection("shops")).getKeys(false)) {
                var section = config.getConfigurationSection("shops." + name);
                if (section == null || !section.isVector("min") || !section.isVector("max")) continue;
                var min = Objects.requireNonNull(section.getVector("min")).toLocation(plugin.getServer().getWorlds().getFirst());
                var max = Objects.requireNonNull(section.getVector("max")).toLocation(plugin.getServer().getWorlds().getFirst());
                UUID owner = getOwnerUUID(name);
                shopRegions.add(new ShopRegion(name, min, max, owner));

                long last = section.getLong("last-updated", 0L);
                if (last > 0) lastUpdatedMap.put(name, last);
            }
        }

        if (config.isConfigurationSection("claims")) {
            for (String uuid : Objects.requireNonNull(config.getConfigurationSection("claims")).getKeys(false)) {
                claimedBy.put(UUID.fromString(uuid), config.getString("claims." + uuid));
            }
        }

        if (config.isConfigurationSection("warps")) {
            for (String shop : Objects.requireNonNull(config.getConfigurationSection("warps")).getKeys(false)) {
                var section = config.getConfigurationSection("warps." + shop);
                if (section == null) continue;
                var world = Bukkit.getWorld(Objects.requireNonNull(section.getString("world")));
                if (world == null) continue;
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                float yaw = (float) section.getDouble("yaw");
                float pitch = (float) section.getDouble("pitch");
                warps.put(shop, new Location(world, x, y, z, yaw, pitch));
            }
        }

        if (config.isConfigurationSection("trusted")) {
            for (String shop : Objects.requireNonNull(config.getConfigurationSection("trusted")).getKeys(false)) {
                List<String> list = config.getStringList("trusted." + shop);
                Set<UUID> uuidSet = new HashSet<>();
                for (String entry : list) {
                    try {
                        uuidSet.add(UUID.fromString(entry));
                    } catch (IllegalArgumentException ignored) {}
                }
                trustedPlayers.put(shop, uuidSet);
            }
        }
    }

    public static void save() {
        config.set("shops", null);
        for (ShopRegion region : shopRegions) {
            String path = "shops." + region.name();
            config.set(path + ".min", region.min().toVector());
            config.set(path + ".max", region.max().toVector());
            if (lastUpdatedMap.containsKey(region.name())) {
                config.set(path + ".last-updated", lastUpdatedMap.get(region.name()));
            }
        }

        config.set("claims", null);
        for (Map.Entry<UUID, String> entry : claimedBy.entrySet()) {
            config.set("claims." + entry.getKey(), entry.getValue());
        }

        config.set("warps", null);
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            String path = "warps." + entry.getKey();
            Location loc = entry.getValue();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
        }

        config.set("trusted", null);
        for (Map.Entry<String, Set<UUID>> entry : trustedPlayers.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                list.add(uuid.toString());
            }
            config.set("trusted." + entry.getKey(), list);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shops.yml: " + e.getMessage());
        }
    }

    public static void addShop(ShopRegion region) {
        shopRegions.add(region);
        save();
    }

    public static boolean isOverlapping(ShopRegion newRegion) {
        return shopRegions.stream().anyMatch(existing -> existing.overlaps(newRegion));
    }

    public static ShopRegion getRegionAt(Location loc) {
        return shopRegions.stream().filter(region -> region.contains(loc)).findFirst().orElse(null);
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
        return claimedBy.entrySet().stream()
                .filter(e -> e.getValue().equals(shopName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static void claimShop(UUID player, String name) {
        claimedBy.put(player, name);
        save();
    }

    public static void setWarp(String shop, Location location) {
        warps.put(shop, location);
        save();
    }

    public static Location getWarp(String shop) {
        return warps.get(shop);
    }

    public static Location getRandomWarp() {
        if (warps.isEmpty()) return null;
        List<Location> values = new ArrayList<>(warps.values());
        return values.get(new Random().nextInt(values.size()));
    }

    public static boolean unclaimShopAt(Location location) {
        ShopRegion region = getRegionAt(location);
        if (region == null) return false;
        UUID owner = getOwnerUUID(region.name());
        if (owner == null) return false;
        claimedBy.remove(owner);
        save();
        return true;
    }

    public static void addTrusted(String shop, UUID uuid) {
        trustedPlayers.computeIfAbsent(shop, k -> new HashSet<>()).add(uuid);
        save();
    }

    public static Set<UUID> getTrusted(String shop) {
        return trustedPlayers.getOrDefault(shop, Collections.emptySet());
    }

    public static void removeTrusted(String shop, UUID uuid) {
        Set<UUID> trusted = trustedPlayers.get(shop);
        if (trusted == null) return;
        boolean removed = trusted.remove(uuid);
        if (removed) save();
    }

    public static boolean isTrusted(String shop, UUID uuid) {
        return trustedPlayers.getOrDefault(shop, Collections.emptySet()).contains(uuid);
    }

    public static boolean isDivine(ShopRegion shop) {
        return "divine".equals(shop.name());
    }

    public static List<ShopRegion> allShops() {
        return new ArrayList<>(shopRegions);
    }

    public static void setLastUpdated(String shop, long time) {
        lastUpdatedMap.put(shop, time);
        save();
    }

    public static Long getLastUpdated(String shop) {
        return lastUpdatedMap.get(shop);
    }

    public static void defineShopRegion(Player player, String name) {
        ShopRegion newRegion = buildRegionFromSelection(player.getUniqueId(), name);
        if (newRegion == null) {
            player.sendMessage("<red>You must select two corners first.");
            return;
        }

        if (isOverlapping(newRegion)) {
            player.sendMessage("<red>This region overlaps another shop.");
            return;
        }

        addShop(newRegion);
        player.sendMessage("<green>Shop '" + name + "' has been defined.");
    }

    private static ShopRegion buildRegionFromSelection(UUID uuid, String name) {
        Location pos1 = ShopSelectionStorage.getPos1(uuid);
        Location pos2 = ShopSelectionStorage.getPos2(uuid);

        if (pos1 == null || pos2 == null || !Objects.equals(pos1.getWorld(), pos2.getWorld())) {
            return null;
        }

        Location min = new Location(pos1.getWorld(),
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()));

        Location max = new Location(pos1.getWorld(),
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ()));

        return new ShopRegion(name, min, max, null);
    }
}
