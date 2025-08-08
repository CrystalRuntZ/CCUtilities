package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.celestialcraft.cCUtilities.modules.ced.DragonUtils;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

public class CelestialDragonListener implements Listener {

    private final JavaPlugin plugin;
    private final DragonManager dragonManager;
    private final Set<UUID> resurrected = new HashSet<>();
    private final Map<UUID, Boolean> lastWither = new HashMap<>();
    private final Random random = new Random();

    public CelestialDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.CELESTIAL)) continue;

                        applyFlightSpeed(dragon);
                        maybeSpawnMinion(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    private void applyFlightSpeed(EnderDragon dragon) {
        if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) return;
        double multiplier = resurrected.contains(dragon.getUniqueId()) ? 2.0 : 1.5;
        dragon.setVelocity(dragon.getVelocity().multiply(multiplier));
    }

    private void maybeSpawnMinion(EnderDragon dragon) {
        if (random.nextDouble() < 0.5) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            EntityType type = switch (random.nextInt(4)) {
                case 0 -> EntityType.VEX;
                case 1 -> EntityType.SKELETON;
                case 2 -> EntityType.WITCH;
                default -> EntityType.BREEZE;
            };

            LivingEntity minion = (LivingEntity) dragon.getWorld().spawnEntity(spawnLoc, type);

            Player target = getNearestPlayer(dragon);
            if (target != null && minion instanceof Mob mob) {
                mob.setTarget(target);
            }

            dragonManager.registerMob(minion);
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
    public void onCelestialDragonDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.CELESTIAL)) return;

        boolean useWither = !lastWither.getOrDefault(dragon.getUniqueId(), false);
        lastWither.put(dragon.getUniqueId(), useWither);

        PotionEffectType effect = useWither ? PotionEffectType.WITHER : PotionEffectType.POISON;
        player.addPotionEffect(new PotionEffect(effect, 60, 0));
        player.getWorld().spawnParticle(
                Particle.WITCH,
                player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.01
        );
    }

    @EventHandler
    public void onCelestialDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.CELESTIAL)) return;

        if (!resurrected.contains(dragon.getUniqueId())) {
            event.setCancelled(true);

            double maxHealth = Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).getValue();
            dragon.setHealth(maxHealth * 0.5);
            resurrected.add(dragon.getUniqueId());

            Location loc = dragon.getLocation();
            World world = dragon.getWorld();
            world.spawnParticle(Particle.END_ROD, loc, 80, 2, 2, 2, 0.1);
            world.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 2f, 0.6f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dragon.isValid()) {
                        dragon.setGlowing(true);
                    }
                }
            }.runTaskLater(plugin, 20L);
        }
    }
}
