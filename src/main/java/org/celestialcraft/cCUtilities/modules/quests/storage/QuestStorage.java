package org.celestialcraft.cCUtilities.modules.quests.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestStorage {

    private static File file;
    private static YamlConfiguration config;
    private static Plugin plugin;

    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        file = new File(plugin.getDataFolder(), "questplayerdata.yml");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("Created questplayerdata.yml for QuestStorage.");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create questplayerdata.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public static void saveAll() {
        if (config == null) return;
        config.options().copyDefaults(true);

        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        Map<UUID, List<Quest>> all = QuestManager.getAll();
        for (Map.Entry<UUID, List<Quest>> entry : all.entrySet()) {
            config.set(entry.getKey().toString(), serializeQuests(entry.getValue()));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save questplayerdata.yml: " + e.getMessage());
        }
    }

    public static void loadAll() {
        if (!file.exists()) return;

        for (String uuidStr : config.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            List<?> rawList = config.getList(uuidStr);
            if (rawList == null) continue;

            List<Quest> quests = new ArrayList<>();
            for (Object obj : rawList) {
                if (!(obj instanceof Map<?, ?> rawMap)) continue;

                try {
                    Quest quest = new Quest(
                            UUID.fromString((String) rawMap.get("id")),
                            (String) rawMap.get("questId"),
                            QuestType.valueOf((String) rawMap.get("type")),
                            ((Number) rawMap.get("target")).intValue(),
                            ((Number) rawMap.get("progress")).intValue(),
                            ((Number) rawMap.get("startTime")).longValue(),
                            ((Number) rawMap.get("expiration")).longValue(),
                            ((Number) rawMap.get("claimWindow")).longValue(),
                            rawMap.containsKey("targetItem") ? (String) rawMap.get("targetItem") : null
                    );
                    quests.add(quest);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load quest for UUID " + uuidStr + ": " + e.getMessage());
                }
            }

            QuestManager.set(uuid, quests);
        }
    }

    private static List<Map<String, Object>> serializeQuests(List<Quest> questList) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Quest quest : questList) {
            Map<String, Object> questMap = new HashMap<>();
            questMap.put("id", quest.getId().toString());
            questMap.put("questId", quest.getQuestId());
            questMap.put("type", quest.getType().name());
            questMap.put("progress", quest.getProgress());
            questMap.put("target", quest.getTarget());
            questMap.put("startTime", quest.getStartTime());
            questMap.put("expiration", quest.getExpirationSeconds());
            questMap.put("claimWindow", quest.getClaimWindowSeconds());
            questMap.put("targetItem", quest.getTargetItem());
            serialized.add(questMap);
        }
        return serialized;
    }

    public static void saveWeeklyQuests(UUID playerId, List<Quest> quests) {
        File file = new File(CCUtilities.getInstance().getDataFolder(), "data/quests/" + playerId + "_weekly.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Quest quest : quests) {
            config.set(quest.getQuestId() + ".type", quest.getType().name());
            config.set(quest.getQuestId() + ".target", quest.getTarget());
            config.set(quest.getQuestId() + ".progress", quest.getProgress());
            config.set(quest.getQuestId() + ".created", quest.getStartTime());
            config.set(quest.getQuestId() + ".duration", quest.getExpirationSeconds() - quest.getStartTime() / 1000);
            config.set(quest.getQuestId() + ".claimWindow", quest.getClaimWindowSeconds());
            config.set(quest.getQuestId() + ".targetItem", quest.getTargetItem());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save weekly quests for " + playerId + ": " + e.getMessage());
        }
    }

    public static List<Quest> loadWeeklyQuests(UUID playerId) {
        File file = new File(CCUtilities.getInstance().getDataFolder(), "data/quests/" + playerId + "_weekly.yml");
        if (!file.exists()) return new ArrayList<>();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Quest> quests = new ArrayList<>();

        for (String questId : config.getKeys(false)) {
            try {
                long created = config.getLong(questId + ".created");
                int duration = config.getInt(questId + ".duration");
                long expiration = created + (duration * 1000L);

                Quest quest = new Quest(
                        UUID.nameUUIDFromBytes((playerId.toString() + "_" + questId).getBytes()),
                        questId,
                        QuestType.valueOf(config.getString(questId + ".type")),
                        config.getInt(questId + ".target"),
                        config.getInt(questId + ".progress"),
                        created,
                        expiration,
                        config.getInt(questId + ".claimWindow"),
                        config.getString(questId + ".targetItem")
                );
                quests.add(quest);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load weekly quest " + questId + " for " + playerId + ": " + e.getMessage());
            }
        }

        return quests;
    }
}
