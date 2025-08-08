package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class EndermanSpawnBlocker implements Listener {

    private final World endWorld;
    private final Location center = new Location(null, 0, 64, 0);
    private static final int RADIUS = 500;

    public EndermanSpawnBlocker(JavaPlugin plugin) {
        this.endWorld = Bukkit.getWorld(plugin.getConfig().getString("spawn-world", "wild_the_end"));
        if (endWorld != null) {
            center.setWorld(endWorld);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEndermanSpawn(CreatureSpawnEvent event) {
        if (!ModuleManager.isEnabled("ced")) return;
        if (!(event.getEntity() instanceof Enderman)) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!event.getLocation().getWorld().equals(endWorld)) return;

        if (event.getLocation().distanceSquared(center) <= RADIUS * RADIUS) {
            event.setCancelled(true);
        }
    }
}
