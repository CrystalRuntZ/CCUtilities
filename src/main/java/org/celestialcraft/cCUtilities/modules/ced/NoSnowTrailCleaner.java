package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Snowman;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class NoSnowTrailCleaner {

    private final NamespacedKey noSnowKey;
    private final Location center;
    private static final int MAX_RADIUS_SQUARED = 500 * 500;

    public NoSnowTrailCleaner(JavaPlugin plugin) {
        this.noSnowKey = new NamespacedKey(plugin, "nosnow");

        World endWorld = Bukkit.getWorld(plugin.getConfig().getString("spawn-world", "wild_the_end"));
        this.center = (endWorld != null) ? new Location(endWorld, 0, 64, 0) : null;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (center == null) return;

                World world = center.getWorld();
                if (world == null) return;

                for (Snowman snowman : world.getEntitiesByClass(Snowman.class)) {
                    if (!snowman.getPersistentDataContainer().has(noSnowKey, PersistentDataType.BYTE)) continue;
                    if (snowman.getLocation().distanceSquared(center) > MAX_RADIUS_SQUARED) continue;

                    cleanSnowAround(snowman);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every 1 second
    }

    private void cleanSnowAround(Snowman snowman) {
        Block center = snowman.getLocation().getBlock();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearby = center.getRelative(x, y, z);
                    if (nearby.getType() == Material.SNOW || nearby.getType() == Material.SNOW_BLOCK) {
                        nearby.setType(Material.AIR);
                    }
                }
            }
        }
    }
}
