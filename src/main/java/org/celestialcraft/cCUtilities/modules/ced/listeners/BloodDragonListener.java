package org.celestialcraft.cCUtilities.modules.ced.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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

import java.util.Objects;
import java.util.Random;

public class BloodDragonListener implements Listener {

    private final JavaPlugin plugin;
    private final DragonManager dragonManager;
    private final Random random = new Random();

    public BloodDragonListener(JavaPlugin plugin, DragonManager dragonManager) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ModuleManager.isEnabled("ced")) return;
                for (World world : Bukkit.getWorlds()) {
                    for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                        if (!DragonUtils.isDragonOfType(dragon, DragonType.BLOOD)) continue;

                        trySpawnVampireBat(dragon);
                        emitBloodTrail(dragon);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    private void trySpawnVampireBat(EnderDragon dragon) {
        if (random.nextDouble() < 0.4) {
            Location spawnLoc = getRandomGroundLocationNearCenter(dragon.getWorld());
            Bat bat = (Bat) dragon.getWorld().spawnEntity(spawnLoc, EntityType.BAT);
            bat.customName(Component.text("vampire_bat"));
            bat.setCustomNameVisible(false);
            bat.setPersistent(true);
            dragonManager.registerMob(bat);
        }
    }

    private void emitBloodTrail(EnderDragon dragon) {
        dragon.getWorld().spawnParticle(
                Particle.DUST,
                dragon.getLocation().add(0, 2, 0),
                20, 0.8, 0.8, 0.8,
                new Particle.DustOptions(Color.fromRGB(130, 0, 0), 1.5f)
        );
    }

    @EventHandler
    public void onBatAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Bat bat)) return;
        if (bat.customName() == null || !Component.text("vampire_bat").equals(bat.customName())) return;
        if (!(event.getEntity() instanceof Player)) return;

        event.setDamage(1.0); // half heart

        EnderDragon nearestBloodDragon = getNearbyBloodDragon(bat);
        if (nearestBloodDragon != null) {
            double maxHealth = Objects.requireNonNull(nearestBloodDragon.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double newHealth = Math.min(maxHealth, nearestBloodDragon.getHealth() + 1.0);
            nearestBloodDragon.setHealth(newHealth);

            bat.getWorld().spawnParticle(Particle.HEART, bat.getLocation(), 3, 0.3, 0.3, 0.3);
        }
    }

    @EventHandler
    public void onDragonAttackPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderDragon dragon)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!DragonUtils.isDragonOfType(dragon, DragonType.BLOOD)) return;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 3 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                player.damage(1.0, dragon); // half a heart per second
                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private EnderDragon getNearbyBloodDragon(Entity source) {
        for (Entity entity : source.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof EnderDragon dragon && DragonUtils.isDragonOfType(dragon, DragonType.BLOOD)) {
                return dragon;
            }
        }
        return null;
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
