package org.celestialcraft.cCUtilities.modules.activity;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
    private final MiniMessage mm = MiniMessage.miniMessage();

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

        // Reward tracker (single source of truth)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                boolean isActive = now - getLastActive(uuid) <= 30 * 60 * 1000;
                long lastReward = pointManager.getLastReward(uuid, isActive);

                long activeInterval = 30 * 60 * 1000L;
                long idleInterval = 60 * 60 * 1000L;
                long interval = isActive ? activeInterval : idleInterval;

                if (now - lastReward >= interval) {
                    int newBal = pointManager.addPoints(player, 1);
                    pointManager.setLastReward(uuid, isActive, now);

                    String typeText = isActive ? "being active" : "idling";
                    String activityText = isActive ? "Active" : "Idle";

                    // Support both legacy %placeholders% and MiniMessage <placeholders>
                    String raw = MessageConfig.get("activity-reward.notify-message")
                            .replace("%type%", typeText)
                            .replace("%activity%", activityText)
                            .replace("%balance%", String.valueOf(newBal));

                    player.sendMessage(mm.deserialize(
                            raw,
                            Placeholder.unparsed("type", typeText),
                            Placeholder.unparsed("activity", activityText),
                            Placeholder.unparsed("balance", String.valueOf(newBal))
                    ));
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

    public void saveAll() {}

    public void reload() {}

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
