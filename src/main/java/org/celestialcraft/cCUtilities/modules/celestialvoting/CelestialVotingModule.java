package org.celestialcraft.cCUtilities.modules.celestialvoting;

import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.commands.CelestialVoteRewardCommand;
import org.celestialcraft.cCUtilities.modules.celestialvoting.config.VotingConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.CommandRegistrar;

public class CelestialVotingModule implements Module {

    private final JavaPlugin plugin;
    private VotingConfig votingConfig;
    private boolean enabled = false;
    private RewardManager rewardManager;


    public CelestialVotingModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        votingConfig = new VotingConfig(plugin);
        this.rewardManager = new RewardManager(votingConfig);

        CommandRegistrar.register(plugin, "celestialvoting", new CelestialVoteRewardCommand(rewardManager), new CelestialVoteRewardCommand(rewardManager));

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
        return "celestialvoting";
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public void reload() {
        if (!enabled) return;
        votingConfig.reload();
    }
}
