package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.List;
import java.util.Random;

public class AetherDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;

    public AetherDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;

                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.AETHER)) continue;

                        applySlowFlight(dragon);
                        maybeSpawnGhast(dragon);
                        maybeSpawnVex(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applySlowFlight(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.setVelocity(dragon.getVelocity().multiply(0.75));
        }
    }

    private void maybeSpawnGhast(EnderDragon dragon) {
        if (random.nextDouble() < 0.2) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Ghast ghast = (Ghast) dragon.getWorld().spawnEntity(spawnLoc, EntityType.GHAST);
            Player target = getNearestPlayer(dragon);
            if (target != null) ghast.setTarget(target);
            dragonManager.registerMob(ghast);
        }
    }

    private void maybeSpawnVex(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Vex vex = (Vex) dragon.getWorld().spawnEntity(spawnLoc, EntityType.VEX);
            Player target = getNearestPlayer(dragon);
            if (target != null) vex.setTarget(target);
            dragonManager.registerMob(vex);
        }
    }

    private Location getRandomGroundLocationNearCenter(World world) {
        Location center = new Location(world, 0, 80, 0);
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 100;
            int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            if (world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                return candidate;
            }
        }
        return center;
    }

    private Player getNearestPlayer(Entity source) {
        List<Player> players = source.getWorld().getPlayers();
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player p : players) {
            double dist = p.getLocation().distanceSquared(source.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                closest = p;
            }
        }
        return closest;
    }
}
