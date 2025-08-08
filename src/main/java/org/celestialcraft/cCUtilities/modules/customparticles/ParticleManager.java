package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class ParticleManager {
    private static final Map<UUID, Set<ParticleEffectType>> activeParticles = new HashMap<>();

    private static final BukkitRunnable particleTask = new BukkitRunnable() {
        @Override
        public void run() {
            for (UUID uuid : activeParticles.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                Set<ParticleEffectType> effects = activeParticles.get(uuid);
                if (effects.contains(ParticleEffectType.FLAME_RING)) {
                    drawFlameRing(player);
                }
            }
        }
    };

    static {
        particleTask.runTaskTimer(CCUtilities.getInstance(), 0L, 5L);
    }

    public static void register(Player player, ParticleEffectType type) {
        activeParticles.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(type);
    }

    public static void unregister(Player player, ParticleEffectType type) {
        Set<ParticleEffectType> effects = activeParticles.get(player.getUniqueId());
        if (effects != null) {
            effects.remove(type);
            if (effects.isEmpty()) {
                activeParticles.remove(player.getUniqueId());
            }
        }
    }

    private static void drawFlameRing(Player player) {
        double radius = 0.8;
        Location loc = player.getLocation().add(0, 0.2, 0);
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0, z), 0, new Vector(0, 0.01, 0));
        }
    }
}
