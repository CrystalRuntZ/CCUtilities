package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class DragonUtils {

    private static NamespacedKey dragonTypeKey;

    public static void initNamespace(JavaPlugin plugin) {
        dragonTypeKey = new NamespacedKey(plugin, "dragon_type");
    }

    public static void setDragonType(EnderDragon dragon, DragonType type) {
        dragon.getPersistentDataContainer().set(dragonTypeKey, PersistentDataType.STRING, type.name());
    }

    public static DragonType getDragonType(EnderDragon dragon) {
        PersistentDataContainer data = dragon.getPersistentDataContainer();
        String stored = data.get(dragonTypeKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try {
            return DragonType.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isDragonOfType(EnderDragon dragon, DragonType type) {
        DragonType actual = getDragonType(dragon);
        return actual == type;
    }

    public static void clearEntitiesOnMainIsland(World endWorld) {
        for (Entity entity : endWorld.getEntities()) {
            if (entity instanceof ExperienceOrb || entity instanceof Player) continue;

            Location loc = entity.getLocation();
            if (Math.abs(loc.getX()) <= 256 && Math.abs(loc.getZ()) <= 256 && loc.getY() <= 256) {
                entity.remove();
            }
        }
    }
}
