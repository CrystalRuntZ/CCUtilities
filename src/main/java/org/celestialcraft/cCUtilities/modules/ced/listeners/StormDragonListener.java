package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.Random;

public class StormDragonListener implements Listener {

    private final Random random = new Random();
    private static final String MAIN_ISLAND_WORLD = "wild_the_end";
    private final DragonManager dragonManager;

    public StormDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                World end = Bukkit.getWorld(MAIN_ISLAND_WORLD);
                if (end == null) return;

                for (EnderDragon dragon : end.getEntitiesByClass(EnderDragon.class)) {
                    if (!DragonUtils.isDragonOfType(dragon, DragonType.STORM)) continue;

                    if (dragon.getPhase() == EnderDragon.Phase.CIRCLING || dragon.getPhase() == EnderDragon.Phase.LAND_ON_PORTAL) {
                        maybeStrikeLightning(end);
                    }

                    if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
                        maybeSpawnBreeze(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void maybeStrikeLightning(World world) {
        if (random.nextDouble() < 0.25) {
            Location strikeLoc = getRandomIslandLocation(world);
            world.strikeLightning(strikeLoc);
        }
    }

    private void maybeSpawnBreeze(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Breeze breeze = (Breeze) dragon.getWorld().spawnEntity(spawnLoc, EntityType.BREEZE);
            dragonManager.registerMob(breeze);

            Player target = getNearestPlayer(dragon);
            if (target != null) {
                breeze.setTarget(target);
            }
        }
    }

    private Location getRandomIslandLocation(World world) {
        int radius = 64;
        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1, z + 0.5);
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
        double closest = Double.MAX_VALUE;
        Player nearest = null;
        for (Player player : source.getWorld().getPlayers()) {
            double dist = player.getLocation().distanceSquared(source.getLocation());
            if (dist < closest) {
                closest = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    @EventHandler
    public void onStormDragonPerchAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.STORM)) return;
        if (dragon.getPhase() != EnderDragon.Phase.LAND_ON_PORTAL) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
    }
}
