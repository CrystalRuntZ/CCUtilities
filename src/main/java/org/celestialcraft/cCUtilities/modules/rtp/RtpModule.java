package org.celestialcraft.cCUtilities.modules.rtp;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class RtpModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public RtpModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        plugin.getLogger().info("RTP module enabled.");
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        plugin.getLogger().info("RTP module disabled.");
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "rtp";
    }

    public void reload() {
        if (!enabled) return;
        plugin.reloadConfig();
    }
}
