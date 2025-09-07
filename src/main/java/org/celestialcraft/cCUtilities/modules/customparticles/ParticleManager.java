package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleManager {
    private static JavaPlugin plugin;
    private static final Map<UUID, EnumSet<ParticleEffectType>> active = new ConcurrentHashMap<>();
    private static volatile int taskId = -1;

    private ParticleManager() {}

    public static void init(JavaPlugin p) {
        if (plugin == null) {
            plugin = p;
        } else if (plugin != p) {
            throw new IllegalStateException("ParticleManager already initialized with a different plugin instance.");
        }
    }

    public static void shutdown() {
        stop();
        active.clear();
    }

    public static void register(Player player, ParticleEffectType type) {
        if (player == null || type == null) return;
        active.compute(player.getUniqueId(), (k, v) -> {
            if (v == null) v = EnumSet.noneOf(ParticleEffectType.class);
            v.add(type);
            return v;
        });
        start();
    }

    public static void unregister(Player player, ParticleEffectType type) {
        if (player == null || type == null) return;
        active.computeIfPresent(player.getUniqueId(), (k, v) -> {
            v.remove(type);
            return v.isEmpty() ? null : v;
        });
        if (active.isEmpty()) stop();
    }

    public static void clearPlayer(Player player) {
        if (player == null) return;
        active.remove(player.getUniqueId());
        if (active.isEmpty()) stop();
    }

    public static boolean isActive(Player player) {
        if (player == null) return false;
        EnumSet<ParticleEffectType> set = active.get(player.getUniqueId());
        return set != null && !set.isEmpty();
    }

    private static void start() {
        if (taskId != -1) return;

        if (plugin == null) {
            // NEW: loud warning so you can spot missing init()
            Bukkit.getLogger().warning("[Particles] Not starting: plugin is null. Did you call ParticleManager.init(plugin) in your module enable()?");
            return;
        }

        plugin.getLogger().info("[Particles] Starting particle task…");
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active.isEmpty()) { stop(); return; }

            final double scalar = ParticleDrawers.tpsScalar();
            var it = active.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, EnumSet<ParticleEffectType>> e = it.next();
                EnumSet<ParticleEffectType> set = e.getValue();
                if (set == null || set.isEmpty()) { it.remove(); continue; }

                Player p = Bukkit.getPlayer(e.getKey());
                if (p == null || !p.isOnline()) { it.remove(); continue; }

                renderEffects(p, set, scalar);
            }
        }, 0L, 2L).getTaskId();
    }

    private static void stop() {
        if (plugin == null || taskId == -1) return;
        plugin.getLogger().info("[Particles] Stopping particle task.");
        Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private static void renderEffects(Player p, EnumSet<ParticleEffectType> set, double scalar) {
        set.forEach(type -> type.render(p, scalar));
    }
}
