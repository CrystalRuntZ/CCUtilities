package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DragonUtils {

    private static NamespacedKey dragonTypeKey;
    private static NamespacedKey fightIdKey;

    public static void initNamespace(JavaPlugin plugin) {
        dragonTypeKey = new NamespacedKey(plugin, "dragon_type");
        fightIdKey    = new NamespacedKey(plugin, "dragon_fight_id");
    }

    // ---- tagging for fight-spawned mobs -------------------------------------

    /** Mark a mob as belonging to a specific dragon fight (by dragon UUID). */
    public static void tagAsFightMob(LivingEntity entity, UUID dragonId) {
        if (entity == null || dragonId == null) return;
        entity.getPersistentDataContainer().set(fightIdKey, PersistentDataType.STRING, dragonId.toString());
    }

    /** Returns the fight UUID this entity belongs to, or null if untagged. */
    public static UUID getFightId(Entity entity) {
        if (entity == null) return null;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String s = pdc.get(fightIdKey, PersistentDataType.STRING);
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException ignored) { return null; }
    }

    /** Is this entity tagged as a fight mob (any fight)? */
    public static boolean isFightMob(Entity entity) {
        return getFightId(entity) != null;
    }

    // ---- dragon type helpers (existing) -------------------------------------

    public static void setDragonType(EnderDragon dragon, DragonType type) {
        dragon.getPersistentDataContainer().set(dragonTypeKey, PersistentDataType.STRING, type.name());
    }

    public static DragonType getDragonType(EnderDragon dragon) {
        PersistentDataContainer data = dragon.getPersistentDataContainer();
        String stored = data.get(dragonTypeKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try { return DragonType.valueOf(stored); } catch (IllegalArgumentException e) { return null; }
    }

    public static boolean isDragonOfType(EnderDragon dragon, DragonType type) {
        DragonType actual = getDragonType(dragon);
        return actual == type;
    }

    // ---- targeted cleanup ---------------------------------------------------

    /** Remove all entities on the main island; keeps players and XP. (Legacy broad broom) */
    public static void  clearEntitiesOnMainIsland(World endWorld) {
        for (Entity entity : endWorld.getEntities()) {
            if (entity instanceof ExperienceOrb || entity instanceof Player) continue;

            Location loc = entity.getLocation();
            if (Math.abs(loc.getX()) <= 256 && Math.abs(loc.getZ()) <= 256 && loc.getY() <= 256) {
                entity.remove();
            }
        }
    }

    /** Remove ONLY tagged fight mobs for a given fight across the entire End world. */
    public static void clearFightMobs(World endWorld, UUID fightId) {
        if (endWorld == null || fightId == null) return;
        for (Entity e : endWorld.getEntities()) {
            if (e instanceof Player || e instanceof EnderDragon || e instanceof ExperienceOrb) continue;
            UUID tag = getFightId(e);
            if (fightId.equals(tag)) e.remove();
        }
    }

    /** Remove all tagged fight mobs (for any fight) across the End world. */
    public static void clearAllFightMobs(World endWorld) {
        if (endWorld == null) return;
        for (Entity e : endWorld.getEntities()) {
            if (e instanceof Player || e instanceof EnderDragon || e instanceof ExperienceOrb) continue;
            if (isFightMob(e)) e.remove();
        }
    }

    /** Remove only tagged fight mobs on the main island, optionally filtering to a specific fight. */
    public static void clearTaggedOnMainIsland(World endWorld, UUID fightIdOrNull) {
        if (endWorld == null) return;
        for (Entity e : endWorld.getEntities()) {
            if (e instanceof Player || e instanceof EnderDragon || e instanceof ExperienceOrb) continue;

            Location loc = e.getLocation();
            if (Math.abs(loc.getX()) > 256 || Math.abs(loc.getZ()) > 256 || loc.getY() > 256) continue;

            UUID tag = getFightId(e);
            if (tag == null) continue;
            if (fightIdOrNull == null || fightIdOrNull.equals(tag)) e.remove();
        }
    }
}
