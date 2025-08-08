package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

public class CrystalDragonListener implements Listener {

    private final JavaPlugin plugin;
    private final DragonManager dragonManager;
    private final Set<UUID> empoweredDragons = new HashSet<>();
    private final Random random = new Random();

    public CrystalDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.CRYSTAL)) continue;

                        checkEmpoweredState(dragon);
                        maybeGoInvisible(dragon);
                        maybeSpawnEndermite(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    private void checkEmpoweredState(EnderDragon dragon) {
        if (empoweredDragons.contains(dragon.getUniqueId())) return;

        double current = dragon.getHealth();
        double max = Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).getValue();

        if (current <= max / 2) {
            empoweredDragons.add(dragon.getUniqueId());
            dragon.setGlowing(true);

            dragon.getWorld().spawnParticle(
                    Particle.END_ROD, dragon.getLocation(),
                    50, 2, 2, 2, 0.05
            );
        }
    }

    private void maybeGoInvisible(EnderDragon dragon) {
        if (random.nextDouble() < 0.05 && !dragon.isInvisible()) {
            dragon.setInvisible(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dragon.isValid()) {
                        dragon.setInvisible(false);
                    }
                }
            }.runTaskLater(plugin, 20L * (1 + random.nextInt(5))); // 1–5 seconds
        }
    }

    private void maybeSpawnEndermite(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Endermite mite = (Endermite) dragon.getWorld().spawnEntity(spawnLoc, EntityType.ENDERMITE);
            Player target = getNearestPlayer(dragon);
            if (target != null) mite.setTarget(target);
            dragonManager.registerMob(mite);
        }
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
    public void onCrystalDragonTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.CRYSTAL)) return;
        if (!empoweredDragons.contains(dragon.getUniqueId())) return;

        event.setDamage(event.getDamage() * 0.5);
    }

    @EventHandler
    public void onCrystalDragonHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.CRYSTAL)) return;
        if (!empoweredDragons.contains(dragon.getUniqueId())) return;

        event.setDamage(event.getDamage() * 1.5);
    }

    @EventHandler
    public void onCrystalDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.CRYSTAL)) return;

        empoweredDragons.remove(dragon.getUniqueId());
    }
}
