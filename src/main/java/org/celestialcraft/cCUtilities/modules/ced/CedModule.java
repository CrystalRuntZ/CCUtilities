package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CedModule implements Module {

    private final JavaPlugin plugin;
    private DragonConfig config;
    private DragonManager dragonManager;
    private RewardDistributor rewardDistributor;
    private DamageTracker damageTracker;
    private boolean enabled = false;

    public CedModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        this.config = new DragonConfig(plugin.getConfig(), plugin);
        this.damageTracker = new DamageTracker();
        BossBarHandler bossBarHandler = new BossBarHandler();
        this.dragonManager = new DragonManager(plugin, config, damageTracker, bossBarHandler);
        this.rewardDistributor = new RewardDistributor(config, damageTracker);

        DragonUtils.initNamespace(plugin);
        AutoDragonSpawner.start(plugin, dragonManager);
        new NoSnowTrailCleaner(plugin);

        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        AutoDragonSpawner.stop();
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "ced";
    }

    public void reload() {
        if (!enabled) return;
        plugin.reloadConfig();
        this.config = new DragonConfig(plugin.getConfig(), plugin);
        this.rewardDistributor = new RewardDistributor(config, damageTracker); // Refresh reward logic
    }

    public DragonConfig getConfig() {
        return config;
    }

    public DragonManager getDragonManager() {
        return dragonManager;
    }

    public RewardDistributor getRewardDistributor() {
        return rewardDistributor;
    }
}
