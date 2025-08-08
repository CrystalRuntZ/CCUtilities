package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.celestialcraft.cCUtilities.MessageConfig;

public class AutoDragonSpawner {

    private static long nextSpawnTimeMillis;
    private static long intervalTicks;
    private static BukkitTask activeTask;

    public static void start(JavaPlugin plugin, DragonManager dragonManager) {
        intervalTicks = plugin.getConfig().getLong("auto-spawn-interval-ticks", 144000); // 2 hours default
        scheduleNext(plugin, dragonManager);
    }

    private static void scheduleNext(JavaPlugin plugin, DragonManager dragonManager) {
        nextSpawnTimeMillis = System.currentTimeMillis() + (intervalTicks * 50L);

        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (dragonManager.isDragonActive()) {
                    plugin.getLogger().warning("[CED] AutoDragonSpawner tried to spawn a dragon, but one is already active.");
                    scheduleNext(plugin, dragonManager);
                    return;
                }

                DragonType type = DragonType.getRandom();
                dragonManager.spawnDragon(type);

                String message = MessageConfig.get("ced.messages.auto-spawn-broadcast")
                        .replace("{name}", dragonManager.getConfig().getName(type));
                Component broadcast = MiniMessage.miniMessage().deserialize(message);
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(broadcast));

                scheduleNext(plugin, dragonManager);
            }
        }.runTaskLater(plugin, intervalTicks);
    }

    public static void stop() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    public static long getTimeUntilNextSpawn() {
        return Math.max(0, nextSpawnTimeMillis - System.currentTimeMillis());
    }
}
