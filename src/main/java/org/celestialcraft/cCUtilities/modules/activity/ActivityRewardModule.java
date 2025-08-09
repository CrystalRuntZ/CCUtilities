package org.celestialcraft.cCUtilities.modules.activity;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.commands.*;
import org.celestialcraft.cCUtilities.listeners.ConfirmationClickListener;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.utils.CommandRegistrar;

public class ActivityRewardModule implements Module {

    private final JavaPlugin plugin;
    private final PlayerActivityTracker tracker;
    private final CelestialPointManager pointManager;
    private final CelestialShopManager shopManager;
    private boolean enabled = false;

    public ActivityRewardModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tracker = new PlayerActivityTracker(plugin);
        this.pointManager = new CelestialPointManager(plugin);
        this.shopManager = new CelestialShopManager(plugin);
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        pointManager.init();
        tracker.initRewards(pointManager);
        tracker.startTracking();

        // Listener registration
        plugin.getServer().getPluginManager().registerEvents(new ConfirmationClickListener(plugin, pointManager), plugin);

        // Command registration
        var activityCommand = new CelestialActivityCommand(pointManager, tracker);
        var shopCommand = new CelestialShopCommand(shopManager);
        var editCommand = new ShopEditCommand(plugin, shopManager);
        var composite = new CompositeCommandExecutor(activityCommand, shopCommand, editCommand);

        CommandRegistrar.register(plugin, "ca", composite, activityCommand);
        CommandRegistrar.register(plugin, "celestialactivity", composite, activityCommand);
        CommandRegistrar.register(plugin, "givepoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());
        CommandRegistrar.register(plugin, "setpoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());
        CommandRegistrar.register(plugin, "removepoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());

        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        tracker.saveAll();
        pointManager.close();
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "activity";
    }

    public void reload() {
        if (!enabled) return;
        tracker.reload();
        pointManager.reload();
        shopManager.reload();
    }

    public ConfirmationGuiManager createConfirmationGui() {
        return new ConfirmationGuiManager(plugin);
    }

    public CelestialShopManager getShopManager() {
        return shopManager;
    }

    public CelestialPointManager getPointManager() {
        return pointManager;
    }

    public PlayerActivityTracker getTracker() {
        return tracker;
    }
}
