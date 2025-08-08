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

public class ToxicDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;

    public ToxicDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.TOXIC)) continue;

                        applyPoisonAura(dragon);
                        applySpeedBoost(dragon);
                        maybeSpawnCaveSpider(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applyPoisonAura(EnderDragon dragon) {
        if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) return;

        for (Player player : dragon.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(dragon.getLocation()) < 9) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        player.getLocation().add(0, 1, 0),
                        15, 0.5, 0.5, 0.5,
                        new Particle.DustOptions(Color.fromRGB(30, 200, 30), 1.2f)
                );
            }
        }
    }

    private void applySpeedBoost(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.setVelocity(dragon.getVelocity().multiply(1.25));
        }
    }

    private void maybeSpawnCaveSpider(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location loc = getRandomGroundLocationNearCenter(dragon.getWorld());
            CaveSpider spider = (CaveSpider) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
            dragonManager.registerMob(spider);

            Player target = getNearestPlayer(dragon);
            if (target != null) {
                spider.setTarget(target);
            }
        }
    }

    private Location getRandomGroundLocationNearCenter(World world) {
        Location center = new Location(world, 0, 80, 0);
        for (int i = 0; i < 10; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 96;
            int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (world.getBlockAt(x, y - 1, z).getType().isSolid()) return candidate;
        }
        return center;
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

    @EventHandler
    public void onToxicDragonPerchAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.TOXIC)) return;
        if (dragon.getPhase() != EnderDragon.Phase.LAND_ON_PORTAL) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 1, 0),
                15, 0.5, 0.5, 0.5,
                new Particle.DustOptions(Color.fromRGB(30, 200, 30), 1.2f)
        );
    }
}
