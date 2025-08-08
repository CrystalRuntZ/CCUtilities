package org.celestialcraft.cCUtilities.modules.entitymanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class EntityManagerModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public EntityManagerModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        if (!plugin.getConfig().getBoolean("entitymanager.enabled", true)) return;

        EntityLimitManager.load(plugin.getConfig());
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        // You might want to clear internal caches here if needed
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "entitymanager";
    }

    public void reload() {
        if (!enabled) return;

        EntityLimitManager.saveAllLimitsToConfig(plugin.getConfig(), plugin);
        plugin.reloadConfig();
        EntityLimitManager.load(plugin.getConfig());
    }
}
