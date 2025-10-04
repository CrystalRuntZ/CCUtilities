package org.celestialcraft.cCUtilities.modules.celestialvoting.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class VotingConfig {

    // --- NEW: singleton bridge so commands can call VotingConfig.reload(main) ---
    private static volatile VotingConfig INSTANCE;

    /** Returns the current singleton instance (may be null if not constructed yet). */
    public static VotingConfig getInstance() { return INSTANCE; }

    /** Ensure an instance exists, then reload it. Safe to call from commands. */
    public static synchronized void reload(JavaPlugin plugin) {
        if (INSTANCE == null) {
            INSTANCE = new VotingConfig(plugin);
        } else {
            INSTANCE.reload();
        }
    }
    // ---------------------------------------------------------------------------

    private final Map<String, List<Map<String, Object>>> rewardsByTier = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> botmRewardsByTier = new HashMap<>();
    private final List<String> tierPriority = new ArrayList<>();

    private final File configFile;
    private YamlConfiguration config;

    public VotingConfig(JavaPlugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "VotingConfig.yml");
        if (!configFile.exists()) {
            plugin.saveResource("VotingConfig.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadRewards();

        // NEW: register as the singleton so static reload() can find us
        INSTANCE = this;
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        rewardsByTier.clear();
        botmRewardsByTier.clear();
        tierPriority.clear();
        loadRewards();
    }

    private void loadRewards() {
        tierPriority.addAll(config.getStringList("vote-tiers"));

        ConfigurationSection rewardSection = config.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String tier : rewardSection.getKeys(false)) {
                List<Map<String, Object>> rewards = new ArrayList<>();
                for (Map<?, ?> raw : rewardSection.getMapList(tier)) {
                    Map<String, Object> entry = new HashMap<>();
                    for (Map.Entry<?, ?> e : raw.entrySet()) {
                        entry.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    rewards.add(entry);
                }
                rewardsByTier.put(tier, rewards);
            }
        }

        ConfigurationSection botmSection = config.getConfigurationSection("botm-rewards");
        if (botmSection != null) {
            for (String tier : botmSection.getKeys(false)) {
                List<Map<String, Object>> rewards = new ArrayList<>();
                for (Map<?, ?> raw : botmSection.getMapList(tier)) {
                    Map<String, Object> entry = new HashMap<>();
                    for (Map.Entry<?, ?> e : raw.entrySet()) {
                        entry.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    rewards.add(entry);
                }
                botmRewardsByTier.put(tier, rewards);
            }
        }
    }

    public List<Map<String, Object>> getRewardsForTier(String tier) {
        return rewardsByTier.getOrDefault(tier, Collections.emptyList());
    }

    public List<Map<String, Object>> getBotmRewardsForTier(String tier) {
        return botmRewardsByTier.getOrDefault(tier, Collections.emptyList());
    }

    public List<String> getVoteTiers() {
        return tierPriority;
    }

    public int getStreakDays() {
        return config.getInt("streak-reward.days", 7);
    }

    public Map<String, Object> getStreakReward() {
        return config.getConfigurationSection("streak-reward") != null
                ? new HashMap<>(Objects.requireNonNull(config.getConfigurationSection("streak-reward")).getValues(false))
                : Collections.emptyMap();
    }

    public boolean isBeginningOfMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day <= 7;
    }
}
