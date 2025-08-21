package org.celestialcraft.cCUtilities.modules.playershops.data;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShopSelectionStorage {
    private ShopSelectionStorage() {}

    private static final Map<UUID, Location> pos1Map = new HashMap<>();
    private static final Map<UUID, Location> pos2Map = new HashMap<>();

    public static void setPos1(UUID uuid, Location location) {
        pos1Map.put(uuid, location);
    }

    public static void setPos2(UUID uuid, Location location) {
        pos2Map.put(uuid, location);
    }

    public static Location getPos1(UUID uuid) {
        return pos1Map.get(uuid);
    }

    public static Location getPos2(UUID uuid) {
        return pos2Map.get(uuid);
    }

    public static boolean hasBoth(UUID uuid) {
        return pos1Map.containsKey(uuid) && pos2Map.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }
}
