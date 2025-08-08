package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

public class EarthDragonListener implements Listener {

    private final Random random = new Random();
    private final Map<UUID, Long> lastSlamTime = new HashMap<>();
    private final DragonManager dragonManager;

    public EarthDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.EARTH)) return;

                handleFlyingParticles(dragon);
                maybeSpawnGolem(dragon);
                maybeBodySlam(dragon);
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void handleFlyingParticles(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.getWorld().spawnParticle(
                    Particle.FALLING_DUST,
                    dragon.getLocation(),
                    30,
                    1.2, 1.2, 1.2,
                    0.02,
                    Bukkit.createBlockData(Material.DIRT)
            );
        }
    }

    private void maybeSpawnGolem(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            IronGolem golem = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
            golem.setHealth(Objects.requireNonNull(golem.getAttribute(Attribute.MAX_HEALTH)).getValue() * 0.25);
            Player target = getNearestPlayer(dragon);
            if (target != null) {
                golem.setTarget(target);
            }
            dragonManager.registerMob(golem);
        }
    }

    private void maybeBodySlam(EnderDragon dragon) {
        long now = System.currentTimeMillis();
        UUID id = dragon.getUniqueId();

        if (lastSlamTime.getOrDefault(id, 0L) + 5000 > now) return;
        if (dragon.getPhase() != EnderDragon.Phase.LAND_ON_PORTAL) return;

        for (Player player : dragon.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(dragon.getLocation()) < 9) {
                Vector launch = player.getLocation().toVector()
                        .subtract(dragon.getLocation().toVector())
                        .normalize()
                        .multiply(2)
                        .setY(1.2);
                player.setVelocity(launch);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.8f);
                player.getWorld().spawnParticle(
                        Particle.FALLING_DUST,
                        player.getLocation(),
                        20, 0.5, 0.5, 0.5,
                        Bukkit.createBlockData(Material.DIRT)
                );
                lastSlamTime.put(id, now);
            }
        }
    }

    private Player getNearestPlayer(Entity source) {
        double closest = Double.MAX_VALUE;
        Player nearest = null;
        for (Player p : source.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(source.getLocation());
            if (dist < closest) {
                closest = dist;
                nearest = p;
            }
        }
        return nearest;
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
