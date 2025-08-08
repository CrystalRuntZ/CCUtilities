package org.celestialcraft.cCUtilities.modules.playershops.data;

import org.bukkit.Location;

import java.util.UUID;

public record ShopRegion(String name, Location min, Location max, UUID owner) {

    public boolean isOwner(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }

    public boolean contains(Location loc) {
        return loc.getWorld().equals(min.getWorld()) &&
                loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX() &&
                loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY() &&
                loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
    }

    public boolean overlaps(ShopRegion other) {
        if (!min.getWorld().equals(other.min().getWorld())) return false;

        return min.getBlockX() <= other.max().getBlockX() && max.getBlockX() >= other.min().getBlockX() &&
                min.getBlockY() <= other.max().getBlockY() && max.getBlockY() >= other.min().getBlockY() &&
                min.getBlockZ() <= other.max().getBlockZ() && max.getBlockZ() >= other.min().getBlockZ();
    }
}
