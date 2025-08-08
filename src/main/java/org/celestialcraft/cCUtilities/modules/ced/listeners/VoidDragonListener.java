package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.Objects;
import java.util.Random;

public class VoidDragonListener implements Listener {

    private final Random random = new Random();
    private final DragonManager dragonManager;

    public VoidDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.VOID)) continue;

                        handleTeleportation(dragon);
                        maybeSpawnMinion(dragon);
                        applyFlightSpeed(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void handleTeleportation(EnderDragon dragon) {
        if (random.nextDouble() < 0.05) {
            Location current = dragon.getLocation();
            double health = dragon.getHealth();

            Location newLoc = current.clone().add(
                    random.nextInt(40) - 20,
                    0,
                    random.nextInt(40) - 20
            );
            newLoc.setY(current.getWorld().getHighestBlockYAt(newLoc) + 10);

            current.getWorld().spawnParticle(Particle.PORTAL, current, 50, 1, 1, 1, 0.2);
            current.getWorld().playSound(current, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 1f);

            dragon.teleport(newLoc);

            double maxHealth = Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).getValue();
            dragon.setHealth(Math.min(health, maxHealth));

            newLoc.getWorld().spawnParticle(Particle.PORTAL, newLoc, 50, 1, 1, 1, 0.2);
            newLoc.getWorld().playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 1f);
        }
    }

    private void maybeSpawnMinion(EnderDragon dragon) {
        if (random.nextDouble() < 0.5) {
            Location loc = getRandomGroundLocationNearCenter(dragon.getWorld());
            EntityType type = random.nextBoolean() ? EntityType.ENDERMAN : EntityType.ENDERMITE;
            LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
            dragonManager.registerMob(mob);

            Player target = getNearestPlayer(dragon);
            if (target != null && mob instanceof Mob m) {
                m.setTarget(target);
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

    private void applyFlightSpeed(EnderDragon dragon) {
        if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
            dragon.setVelocity(dragon.getVelocity().multiply(1.25));
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

    @EventHandler
    public void onVoidDragonPrePerch(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.VOID)) return;

        if (random.nextDouble() > 0.75) {
            event.setCancelled(true);
        }
    }
}
