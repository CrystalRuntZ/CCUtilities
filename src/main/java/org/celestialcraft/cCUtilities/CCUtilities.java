package org.celestialcraft.cCUtilities;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.activity.*;
import org.celestialcraft.cCUtilities.modules.ced.CedModule;
import org.celestialcraft.cCUtilities.modules.celestialvoting.CelestialVotingModule;
import org.celestialcraft.cCUtilities.modules.celestialvoting.VoteStreakTracker;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantsModule;
import org.celestialcraft.cCUtilities.modules.customitems.CustomItemsModule;
import org.celestialcraft.cCUtilities.modules.customparticles.CustomParticlesModule;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityManagerModule;
import org.celestialcraft.cCUtilities.modules.joinitem.JoinItemModule;
import org.celestialcraft.cCUtilities.modules.maparts.MapArtsModule;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModulesConfig;
import org.celestialcraft.cCUtilities.modules.orewatcher.OreWatcherModule;
import org.celestialcraft.cCUtilities.modules.playershops.PlayerShopsModule;
import org.celestialcraft.cCUtilities.modules.quests.QuestModule;
import org.celestialcraft.cCUtilities.modules.randomkeys.RandomKeysModule;
import org.celestialcraft.cCUtilities.modules.referral.ReferralModule;
import org.celestialcraft.cCUtilities.modules.rtp.RtpModule;
import org.celestialcraft.cCUtilities.modules.wordfilter.WordFilterModule;
import org.celestialcraft.cCUtilities.utils.ActivityTracker;
import org.celestialcraft.cCUtilities.utils.CommandRegistry;
import org.celestialcraft.cCUtilities.utils.ListenerRegistry;

public final class CCUtilities extends JavaPlugin {
    public static CCUtilities instance;

    public RandomKeysModule randomKeys;
    public JoinItemModule joinItem;
    public WordFilterModule wordFilter;
    public ReferralModule referralModule;
    public ActivityRewardModule activityModule;
    public RtpModule rtpModule;
    public OreWatcherModule oreWatcherModule;
    public EntityManagerModule entityManagerModule;
    public QuestModule questModule;
    public PlayerShopsModule playerShopsModule;
    public CedModule cedModule;
    public RandomKeysModule randomKeysModule;
    public CelestialVotingModule votingModule;
    public CustomParticlesModule particlesModule;
    public MapArtsModule mapArtsModule;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        MessageConfig.load(this);
        VoteStreakTracker.initialize(getDataFolder());

        randomKeys = new RandomKeysModule(this);
        joinItem = new JoinItemModule(this);
        wordFilter = new WordFilterModule(this);
        referralModule = new ReferralModule(this);
        activityModule = new ActivityRewardModule(this);
        rtpModule = new RtpModule(this);
        oreWatcherModule = new OreWatcherModule(this);
        entityManagerModule = new EntityManagerModule(this);
        questModule = new QuestModule(this);
        playerShopsModule = new PlayerShopsModule(this);
        cedModule = new CedModule(this);
        randomKeysModule = new RandomKeysModule(this);
        votingModule = new CelestialVotingModule(this);
        particlesModule = new CustomParticlesModule();
        ModuleManager.register(particlesModule);

        var customItemsModule = new CustomItemsModule(this);
        var customEnchantsModule = new CustomEnchantsModule(this);
        ModuleManager.register(customItemsModule);
        ModuleManager.register(customEnchantsModule);
        customItemsModule.enable();
        customEnchantsModule.enable();

        mapArtsModule = new MapArtsModule(this);
        ModuleManager.register(mapArtsModule);

        ModulesConfig.reload();
        for (Module module : ModuleManager.getModules()) {
            if (ModulesConfig.shouldEnable(module.getName())) {
                module.enable();
            }
        }

        CommandRegistry.registerAll(this);
        ListenerRegistry.registerAll(this);

        getLogger().info("CCUtilities loaded.");
        ActivityTracker.init("CCUtilities");
    }

    @Override
    public void onDisable() {
        if (questModule != null) questModule.disable();
        if (randomKeys != null) randomKeys.disable();
        if (referralModule != null) referralModule.disable();
        if (activityModule != null) activityModule.disable();
        VoteStreakTracker.close();
    }

    public void reloadAll() {
        reloadConfig();
        randomKeys.enable();
        joinItem.enable();
        wordFilter.enable();
        referralModule.enable();
        activityModule.reload();
        rtpModule.reload();
        oreWatcherModule.reload();
        entityManagerModule.reload();
        questModule.reload();
        cedModule.reload();
        MessageConfig.load(this);
        getLogger().info("All CCUtilities modules and configs reloaded.");
    }

    public static CCUtilities getInstance() {
        return instance;
    }
}
