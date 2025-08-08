package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;

import java.util.*;

public class IceDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;
    private final NamespacedKey noSnowKey;

    public IceDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;
        this.noSnowKey = new NamespacedKey(plugin, "nosnow");

        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.ICE)) continue;

                        applySlowFlight(dragon);
                        maybeSpawnSnowGolem(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applySlowFlight(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.setVelocity(dragon.getVelocity().multiply(0.5));
        }
    }

    private void maybeSpawnSnowGolem(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Snowman golem = (Snowman) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SNOW_GOLEM);
            golem.getPersistentDataContainer().set(noSnowKey, PersistentDataType.BYTE, (byte) 1);

            Player target = getNearestPlayer(dragon);
            if (target != null) {
                golem.setTarget(target);
            }

            dragonManager.registerMob(golem);
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

    @EventHandler
    public void onIceDragonAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.ICE)) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_HIT, 1f, 1.2f);
    }
}
