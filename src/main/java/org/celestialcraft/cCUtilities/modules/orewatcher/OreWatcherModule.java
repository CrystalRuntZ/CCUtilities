package org.celestialcraft.cCUtilities.modules.orewatcher;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.listeners.OreMineListener;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class OreWatcherModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public OreWatcherModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        if (!plugin.getConfig().getBoolean("orewatcher.enabled", true)) return;

        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new OreMineListener(plugin.getConfig()), plugin);

        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "orewatcher";
    }

    public void reload() {
        if (!enabled) return;
        plugin.reloadConfig();
    }
}
