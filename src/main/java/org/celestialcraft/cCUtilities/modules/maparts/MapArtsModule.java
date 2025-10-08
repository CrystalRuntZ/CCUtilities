package org.celestialcraft.cCUtilities.modules.maparts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;

public class MapArtsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled = false;
    private MapArtDataManager dataManager;

    public MapArtsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "maparts";
    }

    @Override
    public void enable() {
        if (enabled) return;
        dataManager = new MapArtDataManager(plugin);
        Bukkit.getPluginManager().registerEvents(new MapArtWandListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new MapArtProtectionListener(dataManager), plugin);
        Bukkit.getPluginManager().registerEvents(new MapArtEntryLockListener(dataManager), plugin);
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public MapArtDataManager getDataManager() {
        return dataManager;
    }
}
