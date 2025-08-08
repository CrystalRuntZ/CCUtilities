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

import java.util.*;

public class ArcaneDragonListener implements Listener {

    private final Set<UUID> vanished = new HashSet<>();
    private final Random random = new Random();
    private final JavaPlugin plugin;
    private final DragonManager dragonManager;

    public ArcaneDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.ARCANE)) continue;

                        handleVanish(dragon);
                        maybeSpawnWitch(dragon);
                        applyPoisonAura(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void handleVanish(EnderDragon dragon) {
        if (!vanished.contains(dragon.getUniqueId()) && random.nextDouble() < 0.1) {
            dragon.setInvisible(true);
            vanished.add(dragon.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    dragon.setInvisible(false);
                    vanished.remove(dragon.getUniqueId());
                }
            }.runTaskLater(plugin, 20L * (1 + random.nextInt(3))); // 1–3 seconds
        }
    }

    private void maybeSpawnWitch(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Witch witch = (Witch) dragon.getWorld().spawnEntity(spawnLoc, EntityType.WITCH);
            Player target = getNearestPlayer(dragon);
            if (target != null) {
                witch.setTarget(target);
            }
            dragonManager.registerMob(witch);
        }
    }

    private void applyPoisonAura(EnderDragon dragon) {
        if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) return;

        for (Player player : dragon.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(dragon.getLocation()) < 9) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 1);
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

    @EventHandler
    public void onArcaneDragonAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.ARCANE)) return;
        if (dragon.getPhase() != EnderDragon.Phase.LAND_ON_PORTAL) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4);
    }
}
