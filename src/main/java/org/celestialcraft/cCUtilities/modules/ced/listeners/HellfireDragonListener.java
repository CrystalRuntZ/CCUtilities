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

public class HellfireDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;

    public HellfireDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.HELLFIRE)) return;

                applyFlameTrail(dragon);
                boostFlightSpeed(dragon);
                maybeSpawnMinion(dragon);
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applyFlameTrail(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.getWorld().spawnParticle(
                    Particle.FLAME,
                    dragon.getLocation(),
                    20, 1.5, 1.5, 1.5, 0.02
            );
        }
    }

    private void boostFlightSpeed(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.setVelocity(dragon.getVelocity().multiply(1.25));
        }
    }

    private void maybeSpawnMinion(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());

            EntityType type = switch (random.nextInt(3)) {
                case 0 -> EntityType.MAGMA_CUBE;
                case 1 -> EntityType.PIGLIN;
                default -> EntityType.BLAZE;
            };

            LivingEntity mob = (LivingEntity) dragon.getWorld().spawnEntity(spawnLoc, type);
            mob.setPersistent(true);

            Player target = getNearestPlayer(dragon);
            if (mob instanceof Mob && target != null) {
                ((Mob) mob).setTarget(target);
            }

            dragonManager.registerMob(mob);
        }
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
}
