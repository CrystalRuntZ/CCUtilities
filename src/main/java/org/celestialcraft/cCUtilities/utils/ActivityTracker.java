package org.celestialcraft.cCUtilities.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityTracker implements Listener {
    private static final Map<UUID, Long> lastActivityMap = new HashMap<>();

    public static void init(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null) {
            Bukkit.getPluginManager().registerEvents(new ActivityTracker(), plugin);
        }
    }

    public static void markActive(Player player) {
        lastActivityMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean isRecentlyActive(Player player, long withinMillis) {
        Long last = lastActivityMap.get(player.getUniqueId());
        return last != null && (System.currentTimeMillis() - last <= withinMillis);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
            markActive(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        markActive(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        markActive(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        markActive(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        markActive(event.getPlayer());
    }
}
