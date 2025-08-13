package org.celestialcraft.cCUtilities.modules.quests.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestTemplate;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.util.*;

public class QuestConfig {

    private static final Map<String, QuestTemplate> templates = new HashMap<>();
    private static final Map<String, List<String>> chains = new HashMap<>();
    private static final Map<String, List<String>> rewardGroups = new HashMap<>(); // NEW

    public static void load(FileConfiguration config) {
        templates.clear();
        chains.clear();
        rewardGroups.clear(); // NEW

        // ----- quests -----
        ConfigurationSection questSection = config.getConfigurationSection("quests");
        if (questSection != null) {
            for (String key : questSection.getKeys(false)) {
                ConfigurationSection section = questSection.getConfigurationSection(key);
                if (section == null) continue;

                QuestType type = QuestType.valueOf(Objects.requireNonNull(section.getString("type")).toUpperCase());
                String display = section.getString("display", key);
                int target = section.getInt("target");
                long expiration = section.getLong("expire_after");
                long claimWindow = section.getLong("claim_window", 600L);
                List<String> rewards = section.getStringList("reward_commands");
                String schedule = section.getString("schedule");
                String targetItem = section.getString("target_item");
                if (targetItem != null) targetItem = targetItem.toUpperCase();

                QuestTemplate template = new QuestTemplate(
                        type,
                        display,
                        target,
                        expiration,
                        claimWindow,
                        rewards,
                        schedule,
                        targetItem
                );

                templates.put(key, template);
            }
        }

        // ----- chains -----
        ConfigurationSection chainsSection = config.getConfigurationSection("chains");
        if (chainsSection != null) {
            for (String chainId : chainsSection.getKeys(false)) {
                List<String> questIds = config.getStringList("chains." + chainId);
                chains.put(chainId, questIds);
            }
        }

        // ----- reward_groups (NEW) -----
        ConfigurationSection groups = config.getConfigurationSection("reward_groups");
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                List<String> cmds = groups.getStringList(key);
                rewardGroups.put(key.toLowerCase(Locale.ROOT), cmds);
            }
        }
    }

    public static QuestTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public static Map<String, QuestTemplate> getAllTemplates() {
        return templates;
    }

    public static List<String> getChain(String chainId) {
        return chains.get(chainId);
    }

    public static Map<String, List<String>> getAllChains() {
        return chains;
    }

    // NEW: access reward groups by id (case-insensitive)
    public static List<String> getRewardGroup(String groupId) {
        if (groupId == null) return Collections.emptyList();
        return rewardGroups.getOrDefault(groupId.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    // (Optional) expose all groups if you ever need to debug/list them
    public static Map<String, List<String>> getAllRewardGroups() {
        return rewardGroups;
    }
}
