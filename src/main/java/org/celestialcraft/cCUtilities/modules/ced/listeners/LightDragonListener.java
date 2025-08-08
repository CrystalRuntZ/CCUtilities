package org.celestialcraft.cCUtilities.modules.ced.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.List;
import java.util.Random;

public class LightDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;

    public LightDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.LIGHT)) return;

                boostFlightSpeed(dragon);
                trySpawnBlazes(dragon);
                tryShootLightBeam(dragon);
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds
    }

    private void boostFlightSpeed(EnderDragon dragon) {
        dragon.setVelocity(dragon.getVelocity().multiply(1.5));
    }

    private void trySpawnBlazes(EnderDragon dragon) {
        EnderDragon.Phase phase = dragon.getPhase();
        if (phase == EnderDragon.Phase.CIRCLING || phase == EnderDragon.Phase.LAND_ON_PORTAL) {
            if (random.nextDouble() < 0.5) {
                Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
                Blaze blaze = (Blaze) dragon.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
                Player target = getNearestPlayer(dragon);
                if (target != null) blaze.setTarget(target);
                dragonManager.registerMob(blaze);
            }
        }
    }

    private void tryShootLightBeam(EnderDragon dragon) {
        if (random.nextDouble() < 0.1) {
            Player target = getNearestPlayer(dragon);
            if (target == null) return;

            Location start = dragon.getLocation().add(0, 4, 0);
            Vector direction = target.getLocation().toVector().subtract(start.toVector()).normalize().multiply(1.2);

            SmallFireball fireball = dragon.getWorld().spawn(start, SmallFireball.class);
            fireball.setVelocity(direction);
            fireball.customName(Component.text("light_beam"));
            fireball.setShooter(dragon);
        }
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

    @EventHandler
    public void onLightBeamHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SmallFireball fireball)) return;
        if (!Component.text("light_beam").equals(fireball.customName())) return;

        if (event.getHitEntity() instanceof Player player) {
            player.damage(1.0, fireball.getShooter() instanceof Entity shooter ? shooter : null);
            player.setFireTicks(60);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2);
        }
    }
}
