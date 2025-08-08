package org.celestialcraft.cCUtilities.modules.ced.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

public class PlagueDragonListener implements Listener {

    private final Map<UUID, Integer> perchCount = new HashMap<>();
    private final Random random = new Random();
    private final Component plagueSandName = Component.text("plague_sand");
    private final DragonManager dragonManager;

    public PlagueDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !DragonUtils.isDragonOfType(dragon, DragonType.PLAGUE)) return;

                handleFlightEffects(dragon);
                trackPerchAndSpeed(dragon);
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void handleFlightEffects(EnderDragon dragon) {
        if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) return;

        if (random.nextDouble() < 0.2) {
            BlockData data = Material.SAND.createBlockData();
            dragon.getWorld().spawn(dragon.getLocation().add(0, -1, 0), FallingBlock.class, block -> {
                block.setBlockData(data);
                block.setDropItem(false);
                block.customName(plagueSandName);
            });
        }

        dragon.getWorld().spawnParticle(
                Particle.FALLING_DUST,
                dragon.getLocation(),
                10, 1, 1, 1,
                Material.GREEN_CONCRETE.createBlockData()
        );
    }

    private void trackPerchAndSpeed(EnderDragon dragon) {
        UUID id = dragon.getUniqueId();
        EnderDragon.Phase phase = dragon.getPhase();

        if (phase == EnderDragon.Phase.LAND_ON_PORTAL) {
            perchCount.put(id, perchCount.getOrDefault(id, 0) + 1);
        } else if (phase == EnderDragon.Phase.CIRCLING) {
            int boost = perchCount.getOrDefault(id, 0);
            double multiplier = 1.0 + (boost * 0.10);
            dragon.setVelocity(dragon.getVelocity().multiply(multiplier));
        }
    }

    @EventHandler
    public void onPlagueDragonHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Entity damager = event.getDamager();
        EnderDragon dragon = null;

        if (damager instanceof EnderDragon) {
            dragon = (EnderDragon) damager;
        } else if (damager instanceof ComplexEntityPart part && part.getParent() instanceof EnderDragon parent) {
            dragon = parent;
        }

        if (dragon == null) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.PLAGUE)) return;
        if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));

        player.getWorld().spawnParticle(
                Particle.FALLING_DUST,
                player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5,
                Material.GREEN_CONCRETE_POWDER.createBlockData()
        );
    }

    @EventHandler
    public void onPlagueSandFall(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof FallingBlock block)) return;
        if (!plagueSandName.equals(block.customName())) return;
        if (!(event.getEntity() instanceof Player)) return;

        event.setDamage(1.0);
        block.remove();
    }

    @EventHandler
    public void onSandLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock block)) return;
        if (!plagueSandName.equals(block.customName())) return;

        event.setCancelled(true);
        block.remove();
    }

    @EventHandler
    public void onPlagueDragonPerchedDamageTick(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.PLAGUE)) return;

        if (dragon.getPhase() == EnderDragon.Phase.LAND_ON_PORTAL && random.nextDouble() < 0.6) {
            World world = dragon.getWorld();
            Location spawnLoc = getRandomGroundLocationNearCenter(world);

            SkeletonHorse horse = (SkeletonHorse) world.spawnEntity(spawnLoc, EntityType.SKELETON_HORSE);
            horse.setTamed(true);
            dragonManager.registerMob(horse);

            Skeleton rider = (Skeleton) world.spawnEntity(spawnLoc, EntityType.SKELETON);
            dragonManager.registerMob(rider);

            rider.addPassenger(horse);

            Player target = getNearestPlayer(dragon);
            if (target != null) {
                rider.setTarget(target);
            }
        }
    }

    private Player getNearestPlayer(Entity source) {
        List<Player> players = source.getWorld().getPlayers();
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player p : players) {
            double dist = p.getLocation().distanceSquared(source.getLocation());
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
}
