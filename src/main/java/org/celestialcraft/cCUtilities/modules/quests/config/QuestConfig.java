package org.celestialcraft.cCUtilities.modules.quests.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestTemplate;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.util.*;

public class QuestConfig {

    private static final Map<String, QuestTemplate> templates = new HashMap<>();
    private static final Map<String, List<String>> chains = new HashMap<>();

    public static void load(FileConfiguration config) {
        templates.clear();
        chains.clear();

        ConfigurationSection questSection = config.getConfigurationSection("quests");
        if (questSection == null) return;

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

        ConfigurationSection chainsSection = config.getConfigurationSection("chains");
        if (chainsSection != null) {
            for (String chainId : chainsSection.getKeys(false)) {
                List<String> questIds = config.getStringList("chains." + chainId);
                chains.put(chainId, questIds);
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
}
