package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.PlayerActivityTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.UUID;

public class ActivityListener implements Listener {

    private final PlayerActivityTracker tracker;
    private final CelestialPointManager pointManager;

    public ActivityListener(PlayerActivityTracker tracker, CelestialPointManager pointManager, JavaPlugin plugin) {
        this.tracker = tracker;
        this.pointManager = pointManager;

        if (!ModuleManager.isEnabled("activity")) return;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAndReward(player);
            }
        }, 20L * 60, 20L * 60);
    }

    private void checkAndReward(Player player) {
        if (!ModuleManager.isEnabled("activity")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        long lastMove = tracker.getLastActive(uuid);
        boolean isActive = now - lastMove <= 5 * 60 * 1000L;

        long lastReward = pointManager.getLastReward(uuid, isActive);
        long activeInterval = 30 * 60 * 1000L;
        long idleInterval = 60 * 60 * 1000L;
        long interval = isActive ? activeInterval : idleInterval;

        if (now - lastReward >= interval) {
            pointManager.addPoints(player, 1);
            pointManager.setLastReward(uuid, isActive, now);

            String typeText = isActive ? "being active" : "idling";
            String rawMessage = MessageConfig.get("activity-reward.notify-message")
                    .replace("%type%", typeText)
                    .replace('&', '§');

            player.sendMessage(rawMessage);
        }
    }
}
