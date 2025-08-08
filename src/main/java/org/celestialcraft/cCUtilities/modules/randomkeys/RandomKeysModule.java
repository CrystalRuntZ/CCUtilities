package org.celestialcraft.cCUtilities.modules.randomkeys;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class RandomKeysModule implements Module {

    private final JavaPlugin plugin;
    private KeyDistributor keyDistributor;
    private KeyDataManager keyDataManager;
    private boolean enabled = false;

    public RandomKeysModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        enabled = plugin.getConfig().getBoolean("random-keys.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("RandomKeys module is disabled in config.");
            return;
        }

        keyDataManager = new KeyDataManager(plugin);
        keyDistributor = new KeyDistributor(plugin, keyDataManager);
        keyDistributor.scheduleHourlyDistribution();
        keyDistributor.scheduleDailyReset();

        plugin.getLogger().info("RandomKeys module enabled.");
    }

    @Override
    public void disable() {
        if (!enabled) return;

        if (keyDataManager != null) {
            keyDataManager.save();
        }

        enabled = false;
        plugin.getLogger().info("RandomKeys module disabled.");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "randomkeys";
    }

    public KeyDataManager getKeyDataManager() {
        return keyDataManager;
    }

    public KeyDistributor getKeyDistributor() {
        return keyDistributor;
    }
}
