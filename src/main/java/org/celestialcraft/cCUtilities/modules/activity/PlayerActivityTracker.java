package org.celestialcraft.cCUtilities.modules.activity;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerActivityTracker {

    private final JavaPlugin plugin;
    private final Map<UUID, ActivityData> activityMap = new HashMap<>();
    private CelestialPointManager pointManager;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public PlayerActivityTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTracking() {
        long taskInterval = 20L * 60;

        // Movement tracker
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Triple loc = new Triple(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());

                ActivityData data = activityMap.computeIfAbsent(uuid, k -> new ActivityData(loc, now, now));

                boolean moved = !data.lastLocation.equals(loc);
                data.lastLocation = loc;

                if (moved) {
                    data.lastActive = now;
                } else if (now - data.lastActive >= 30 * 60 * 1000) {
                    data.lastIdle = now;
                }
            }
        }, 0L, taskInterval);

        // Reward tracker
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                long lastActiveReward = pointManager.getLastReward(uuid, true);
                if (now - getLastActive(uuid) <= 30 * 60 * 1000 && now - lastActiveReward >= 30 * 60 * 1000) {
                    pointManager.addPoints(player, 1);
                    pointManager.setLastReward(uuid, true, now);

                    String msg = MessageConfig.get("activity-reward.notify-message").replace("%type%", "being active");
                    player.sendMessage(legacy.deserialize(msg));
                }

                long lastIdleReward = pointManager.getLastReward(uuid, false);
                if (now - getLastActive(uuid) > 30 * 60 * 1000 && now - lastIdleReward >= 60 * 60 * 1000) {
                    pointManager.addPoints(player, 1);
                    pointManager.setLastReward(uuid, false, now);

                    String msg = MessageConfig.get("activity-reward.notify-message").replace("%type%", "idling");
                    player.sendMessage(legacy.deserialize(msg));
                }
            }
        }, taskInterval, taskInterval);
    }

    public void initRewards(CelestialPointManager manager) {
        this.pointManager = manager;
    }

    public long getLastActive(UUID uuid) {
        ActivityData data = activityMap.get(uuid);
        return data != null ? data.lastActive : System.currentTimeMillis();
    }

    public void saveAll() {
        // future persistence support
    }

    public void reload() {
        // reload tracking logic if needed
    }

    private static class ActivityData {
        Triple lastLocation;
        long lastActive;
        long lastIdle;

        ActivityData(Triple lastLocation, long lastActive, long lastIdle) {
            this.lastLocation = lastLocation;
            this.lastActive = lastActive;
            this.lastIdle = lastIdle;
        }
    }

    private record Triple(double x, double y, double z) {}
}
