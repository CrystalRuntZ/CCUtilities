    package org.celestialcraft.cCUtilities.modules.ced.listeners;

    import org.bukkit.*;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.Listener;
    import org.bukkit.event.entity.EntityDamageByEntityEvent;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.scheduler.BukkitRunnable;
    import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
    import org.celestialcraft.cCUtilities.modules.ced.DragonType;
    import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
    import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

    import java.util.*;

    public class FireDragonListener implements Listener {

        private final Random random = new Random();
        private final DragonManager dragonManager;

        public FireDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
            this.dragonManager = dragonManager;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!ModuleManager.isEnabled("ced")) return;
                    EnderDragon dragon = dragonManager.getActiveDragon();
                    if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.FIRE)) return;

                    if (dragon.getPhase() == EnderDragon.Phase.CIRCLING) {
                        maybeSpawnBlaze(dragon);
                    } else if (dragon.getPhase() == EnderDragon.Phase.LAND_ON_PORTAL) {
                        maybeSpawnMagmaCube(dragon);
                    }
                }
            }.runTaskTimer(plugin, 0L, 40L);
        }

        private void maybeSpawnBlaze(EnderDragon dragon) {
            if (random.nextDouble() < 0.5) {
                Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
                Blaze blaze = (Blaze) dragon.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
                Player target = getNearestPlayer(dragon);
                if (target != null) {
                    blaze.setTarget(target);
                }
                dragonManager.registerMob(blaze);
            }
        }

        private void maybeSpawnMagmaCube(EnderDragon dragon) {
            if (random.nextDouble() < 0.5) {
                Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
                MagmaCube cube = (MagmaCube) dragon.getWorld().spawnEntity(spawnLoc, EntityType.MAGMA_CUBE);
                cube.setSize(2 + random.nextInt(3));
                Player target = getNearestPlayer(dragon);
                if (target != null) {
                    cube.setTarget(target);
                }
                dragonManager.registerMob(cube);
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
        public void onFireDragonAttack(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof EnderDragon dragon)) return;
            if (!(event.getEntity() instanceof Player player)) return;
            if (!DragonUtils.isDragonOfType(dragon, DragonType.FIRE)) return;

            player.setFireTicks(60);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.01);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1f, 1f);
        }
    }
