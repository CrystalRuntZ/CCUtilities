package org.celestialcraft.cCUtilities.modules.maparts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class ResourceRegion {
    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private Material fillMaterial;

    public ResourceRegion(String name, String worldName, int minX, int minY, int minZ,
                          int maxX, int maxY, int maxZ, Material fillMaterial) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.fillMaterial = fillMaterial == null ? Material.RED_CONCRETE : fillMaterial;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public Material getFillMaterial() { return fillMaterial; }
    public void setFillMaterial(Material m) { this.fillMaterial = m; }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public World world() { return Bukkit.getWorld(worldName); }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public long volume() {
        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
