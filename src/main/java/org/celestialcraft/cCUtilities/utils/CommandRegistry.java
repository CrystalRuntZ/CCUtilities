package org.celestialcraft.cCUtilities.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.commands.*;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.CelestialShopManager;
import org.celestialcraft.cCUtilities.modules.activity.PlayerActivityTracker;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.celestialvoting.RewardManager;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityManagerModule;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

// NEW: resource regions (maparts)
import org.celestialcraft.cCUtilities.modules.maparts.ResourceRegionManager;
import org.celestialcraft.cCUtilities.modules.maparts.ResourceRegionListener;
import org.celestialcraft.cCUtilities.modules.maparts.ResourceRegionCommand;

public class CommandRegistry {

    public static void registerAll(JavaPlugin plugin) {
        var instance = CCUtilities.getInstance();

        // Always available
        CommandRegistrar.register(plugin, "celestial", new CelestialCommand(instance), null);
        CommandRegistrar.register(plugin, "ccmodules", new ModuleToggleCommand(plugin), new ModuleToggleCommand(plugin));
        CommandRegistrar.register(plugin, "unityaccept", new UnityAcceptCommand(), null);
        CommandRegistrar.register(plugin, "unitydeny", new UnityDenyCommand(), null);
        CommandRegistrar.register(plugin, "rawbc", new RawBroadcastCommand(), null);

        // Quests
        if (ModuleManager.isEnabled("quests")) {
            CommandRegistrar.register(plugin, "quests", new QuestsCommand(), new QuestsTabCompleter());
        }

        // Random Keys
        if (ModuleManager.isEnabled("randomkeys")) {
            CommandRegistrar.register(plugin, "randomkeys", new RandomKeysCommand(plugin, instance.randomKeysModule), null);
        }

        // Rank Select
        if (ModuleManager.isEnabled("rankselect")) {
            CommandRegistrar.register(plugin, "rankselect", new RankSelectCommand(), new RankSelectCommand());
        }

        // Celestial Activity Module
        if (ModuleManager.isEnabled("celestialactivity")) {
            CelestialPointManager pointManager = instance.activityModule.getPointManager();
            PlayerActivityTracker tracker = instance.activityModule.getTracker();
            CelestialShopManager shopManager = instance.activityModule.getShopManager();

            var balanceCmd = new CelestialActivityCommand(pointManager, tracker);
            var shopCmd = new CelestialShopCommand(shopManager);
            var editCmd = new ShopEditCommand(plugin, shopManager);
            var composite = new CompositeCommandExecutor(balanceCmd, shopCmd, editCmd);

            CommandRegistrar.register(plugin, "celestialactivity", composite, balanceCmd);
            CommandRegistrar.register(plugin, "ca", composite, balanceCmd);

            CommandRegistrar.register(plugin, "givepoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());
            CommandRegistrar.register(plugin, "setpoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());
            CommandRegistrar.register(plugin, "removepoints", new CelestialPointsAdminCommand(pointManager), new CelestialPointsAdminTabCompleter());
        }

        // RTP Commands
        if (ModuleManager.isEnabled("rtp")) {
            CommandRegistrar.register(plugin, "rtp", new RtpCommand(), null);
            CommandRegistrar.register(plugin, "wild", new RtpCommand(), null);
            CommandRegistrar.register(plugin, "nether", new RtpCommand(), null);
            CommandRegistrar.register(plugin, "end", new RtpCommand(), null);
        }

        // Custom Ender Dragon Module
        if (ModuleManager.isEnabled("ced")) {
            DragonManager dragonManager = instance.cedModule.getDragonManager();
            CedCommand cedCmd = new CedCommand(dragonManager);
            CommandRegistrar.register(plugin, "ced", cedCmd, cedCmd);
        }

        // MapArts + Resource Regions
        if (ModuleManager.isEnabled("maparts")) {
            var mapartCmd = new org.celestialcraft.cCUtilities.modules.maparts.MapArtMainCommand(
                    plugin,
                    org.celestialcraft.cCUtilities.CCUtilities.getInstance().mapArtsModule.getDataManager()
            );
            CommandRegistrar.register(plugin, "mapart", mapartCmd, mapartCmd);

            // NEW: Resource Regions (/mapres) registration + listener
            ResourceRegionManager rrManager = new ResourceRegionManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(new ResourceRegionListener(rrManager), plugin);

            var mapresCmd = new ResourceRegionCommand(rrManager);
            CommandRegistrar.register(plugin, "mapres", mapresCmd, mapresCmd);
        }

        // Celestial Voting Module
        if (ModuleManager.isEnabled("celestialvoting")) {
            RewardManager rewardManager = instance.votingModule.getRewardManager();
            var votingCommand = new CelestialVoteRewardCommand(rewardManager);
            CommandRegistrar.register(plugin, "celestialvoting", votingCommand, votingCommand);
        }

        // Entity Manager Module
        if (ModuleManager.isEnabled("entitymanager")) {
            EntityManagerModule entityManager = instance.entityManagerModule;
            CommandRegistrar.register(plugin, "entitymanager",
                    new EntityManagerCommand(plugin, entityManager),
                    new EntityManagerTabCompleter());
        }

        if (ModuleManager.isEnabled("playershops")) {
            var shopsCmd = new ShopsMainCommand();
            CommandRegistrar.register(plugin, "shops", shopsCmd, shopsCmd);

            // Aliases
            CommandRegistrar.register(plugin, "shop", new ShopWarpCommand(), new ShopWarpCommand());
            CommandRegistrar.register(plugin, "setshop", new SetShopCommand(), null);
        }

        // Referral System
        if (ModuleManager.isEnabled("referral")) {
            CommandRegistrar.register(plugin, "referral", new ReferralCommand(plugin, instance.referralModule.getDatabase()), null);
        }

        // === Backpack admin command (universal for backpack-style items) ===
        // single instance used for executor and tab completer
        var backpackCmd = new BackpackCommand(plugin);
        CommandRegistrar.register(plugin, "backpack", backpackCmd, backpackCmd);
    }
}
