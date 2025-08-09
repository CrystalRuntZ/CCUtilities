package org.celestialcraft.cCUtilities.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.listeners.*;
import org.celestialcraft.cCUtilities.modules.activity.ConfirmationGuiManager;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.PlayerActivityTracker;
import org.celestialcraft.cCUtilities.modules.ced.*;
import org.celestialcraft.cCUtilities.modules.ced.listeners.DragonDeathListener;
import org.celestialcraft.cCUtilities.modules.ced.listeners.*;
import org.celestialcraft.cCUtilities.modules.joinitem.JoinItemModule;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.listeners.*;
import org.celestialcraft.cCUtilities.modules.wordfilter.WordFilterModule;

public class ListenerRegistry {

    public static void registerAll(JavaPlugin plugin) {
        var activity = CCUtilities.getInstance().activityModule;
        CelestialPointManager pointManager = activity.getPointManager();
        PlayerActivityTracker tracker = activity.getTracker();
        ConfirmationGuiManager confirmationGui = new ConfirmationGuiManager(plugin);

        if (ModuleManager.isEnabled("celestialactivity")) {
            plugin.getServer().getPluginManager().registerEvents(new ShopClickListener(plugin, pointManager, confirmationGui), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ConfirmationClickListener(plugin, pointManager), plugin);
        }

        if (ModuleManager.isEnabled("entitymanager")) {
            plugin.getServer().getPluginManager().registerEvents(new BlockLimitListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new BlockPlaceListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new EntityLimiterListener(), plugin);
        }

        if (ModuleManager.isEnabled("customenchants")) {
            plugin.getServer().getPluginManager().registerEvents(new CustomEnchantAnvilListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new CustomEnchantAnvilClickListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new CustomEnchantEffectListener(), plugin);
        }

        if (ModuleManager.isEnabled("customitems")) {
            plugin.getServer().getPluginManager().registerEvents(new CustomItemEffectListener(), plugin);
        }

        if (ModuleManager.isEnabled("orewatcher")) {
            plugin.getServer().getPluginManager().registerEvents(new OreMineListener(plugin.getConfig()), plugin);
        }

        if (ModuleManager.isEnabled("playershops")) {
            plugin.getServer().getPluginManager().registerEvents(new ShopActivityListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ShopBuildListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ShopChestAccessListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ShopChestListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ShopSelectionListener(CCUtilities.getInstance().playerShopsModule), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ShopWandListener(), plugin);
        }

        if (ModuleManager.isEnabled("joinitem")) {
            plugin.getServer().getPluginManager().registerEvents(new JoinItemModule(plugin), plugin);
        }

        if (ModuleManager.isEnabled("referral")) {
            plugin.getServer().getPluginManager().registerEvents(CCUtilities.getInstance().referralModule.gui, plugin);
        }

        if (ModuleManager.isEnabled("wordfilter")) {
            plugin.getServer().getPluginManager().registerEvents(new WordFilterModule(plugin), plugin);
        }

        if (ModuleManager.isEnabled("quests")) {
            plugin.getServer().getPluginManager().registerEvents(new BiomeListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new BlockBreakListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new BreedListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new DamageListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new GlideListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new HarvestListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new MobKillListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new PlaceBlockListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new RunListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new SmeltListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new SwimListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new XPListener(), plugin);
        }

        if (ModuleManager.isEnabled("ced")) {
            registerCed(plugin);
        }
    }

    public static void registerCed(JavaPlugin plugin) {
        var ced = CCUtilities.getInstance().cedModule;
        DragonConfig config = ced.getConfig();
        DragonManager dragonManager = ced.getDragonManager();

        plugin.getServer().getPluginManager().registerEvents(new DragonDamageListener(dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DragonDeathListener(dragonManager, ced.getRewardDistributor()), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AetherDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ArcaneDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BloodDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CelestialDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CrystalDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EarthDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new FireDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new HellfireDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new IceDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new LightDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlagueDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SandDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ShadowDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new StormDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ToxicDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new VoidDragonListener(plugin, dragonManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EndermanSpawnBlocker(plugin), plugin);
    }
}
