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

public class ShadowDragonListener implements Listener {

    private final JavaPlugin plugin;
    private final DragonManager dragonManager;
    private final Random random = new Random();

    public ShadowDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.SHADOW)) continue;

                        emitParticles(dragon);
                        tryInvisibilityTeleport(dragon);
                        trySummonEndermen(dragon);
                        trySummonCreakingMob(dragon);
                        slowFlight(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void emitParticles(EnderDragon dragon) {
        Location loc = dragon.getLocation();
        World world = dragon.getWorld();
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 20, 2, 2, 2, 0.01);
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 10, 1.5, 1.5, 1.5);
        world.spawnParticle(Particle.SMOKE, loc, 10, 1.5, 1.5, 1.5);
    }

    private void tryInvisibilityTeleport(EnderDragon dragon) {
        if (random.nextDouble() < 0.1) {
            dragon.setInvisible(true);
            Location original = dragon.getLocation();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!dragon.isValid()) return;
                    World world = dragon.getWorld();
                    Location newLoc = original.clone().add(random.nextInt(30) - 15, 0, random.nextInt(30) - 15);
                    newLoc.setY(Math.max(70, world.getHighestBlockYAt(newLoc) + 10));
                    dragon.teleport(newLoc);
                    dragon.setInvisible(false);
                }
            }.runTaskLater(plugin, 20L + random.nextInt(60));
        }
    }

    private void trySummonEndermen(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            World world = dragon.getWorld();
            Location spawnLoc = getRandomGroundLocationNearCenter(world);
            Enderman enderman = (Enderman) world.spawnEntity(spawnLoc, EntityType.ENDERMAN);
            dragonManager.registerMob(enderman);

            Player target = getNearestPlayer(dragon);
            if (target != null) enderman.setTarget(target);
        }
    }

    private void trySummonCreakingMob(EnderDragon dragon) {
        EnderDragon.Phase phase = dragon.getPhase();
        if (phase == EnderDragon.Phase.LAND_ON_PORTAL || phase == EnderDragon.Phase.CIRCLING) {
            if (random.nextDouble() < 0.2) {
                World world = dragon.getWorld();
                Location spawnLoc = getRandomGroundLocationNearCenter(world);

                LivingEntity creaking = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.CREAKING);
                dragonManager.registerMob(creaking);

                if (creaking instanceof Mob mob) {
                    Player target = getNearestPlayer(dragon);
                    if (target != null) mob.setTarget(target);
                }
            }
        }
    }

    private void slowFlight(EnderDragon dragon) {
        dragon.setVelocity(dragon.getVelocity().multiply(0.5));
    }

    private Player getNearestPlayer(Entity entity) {
        List<Player> players = entity.getWorld().getPlayers();
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player p : players) {
            double dist = p.getLocation().distanceSquared(entity.getLocation());
            if (dist < closestDist) {
                closest = p;
                closestDist = dist;
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
