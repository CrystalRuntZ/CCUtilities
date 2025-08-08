package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.List;
import java.util.Random;

public class SandDragonListener implements Listener {

    private final Random random = new Random();
    private final NamespacedKey cactusKey;
    private final DragonManager dragonManager;

    public SandDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.cactusKey = new NamespacedKey(plugin, "sand_cactus");
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.SAND)) return;

                emitDustTrail(dragon);
                tryShootCactus(dragon);
                trySummonHusk(dragon);
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void emitDustTrail(EnderDragon dragon) {
        Location loc = dragon.getLocation();
        World world = dragon.getWorld();

        world.spawnParticle(Particle.BLOCK_CRUMBLE, loc, 20, 1.5, 1, 1.5, 0.1, Material.SAND.createBlockData());
        world.spawnParticle(Particle.FALLING_DUST, loc, 10, 1, 1, 1, Material.SAND.createBlockData());
    }

    private void tryShootCactus(EnderDragon dragon) {
        if (random.nextDouble() < 0.08) {
            Player target = getNearestPlayer(dragon);
            if (target == null) return;

            Location start = dragon.getLocation().add(0, 4, 0);
            Vector direction = target.getLocation().toVector().subtract(start.toVector()).normalize().multiply(1.5);

            FallingBlock cactus = dragon.getWorld().spawnFallingBlock(start, Material.CACTUS.createBlockData());
            cactus.setVelocity(direction);
            cactus.setDropItem(false);
            cactus.setHurtEntities(false);
            cactus.getPersistentDataContainer().set(cactusKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void trySummonHusk(EnderDragon dragon) {
        EnderDragon.Phase phase = dragon.getPhase();
        if (phase == EnderDragon.Phase.CIRCLING || phase == EnderDragon.Phase.LAND_ON_PORTAL) {
            if (random.nextDouble() < 0.4) {
                Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
                Husk husk = (Husk) dragon.getWorld().spawnEntity(spawnLoc, EntityType.HUSK);
                dragonManager.registerMob(husk);

                Player target = getNearestPlayer(dragon);
                if (target != null) husk.setTarget(target);
            }
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
    public void onCactusHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof FallingBlock block)) return;
        if (!block.getPersistentDataContainer().has(cactusKey, PersistentDataType.BYTE)) return;
        if (!(event.getEntity() instanceof Player)) return;

        event.setDamage(2.0);
        block.remove();
    }
}
