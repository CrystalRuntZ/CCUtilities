package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleManager {
    private static JavaPlugin plugin;
    private static final Map<UUID, EnumSet<ParticleEffectType>> active = new ConcurrentHashMap<>();
    private static int taskId = -1;

    public static void init(JavaPlugin p) {
        if (plugin == null) plugin = p;
    }

    public static void register(Player player, ParticleEffectType type) {
        active.compute(player.getUniqueId(), (k, v) -> {
            if (v == null) v = EnumSet.noneOf(ParticleEffectType.class);
            v.add(type);
            return v;
        });
        start();
    }

    public static void unregister(Player player, ParticleEffectType type) {
        active.computeIfPresent(player.getUniqueId(), (k, v) -> {
            v.remove(type);
            return v.isEmpty() ? null : v;
        });
        if (active.isEmpty()) stop();
    }

    private static void start() {
        if (plugin == null || taskId != -1) return;
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (active.isEmpty()) return;
            for (UUID id : active.keySet()) {
                Player p = plugin.getServer().getPlayer(id);
                if (p == null || !p.isOnline()) continue;
                EnumSet<ParticleEffectType> set = active.get(id);
                if (set == null || set.isEmpty()) continue;
                if (set.contains(ParticleEffectType.FLAME_RING)) drawFlameRing(p);
            }
        }, 0L, 2L).getTaskId();
    }

    private static void stop() {
        if (plugin == null || taskId == -1) return;
        plugin.getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private static void drawFlameRing(Player p) {
        Location loc = p.getLocation();
        double y = loc.getY() + 0.1;
        double r = 0.8;
        int points = 24;
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points;
            double x = loc.getX() + Math.cos(a) * r;
            double z = loc.getZ() + Math.sin(a) * r;
            p.getWorld().spawnParticle(Particle.FLAME, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}
