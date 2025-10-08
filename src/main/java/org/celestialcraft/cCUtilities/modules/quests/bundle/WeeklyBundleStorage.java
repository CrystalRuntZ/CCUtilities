package org.celestialcraft.cCUtilities.modules.quests.bundle;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class WeeklyBundleStorage {
    private static final String ROOT = "bundles";
    private static final String OWNER = "owner";
    private static final String EXPIRES = "expires";
    private static final String QUESTS = "quests";

    // quest map keys
    private static final String Q_ID          = "id";
    private static final String Q_QUEST_ID    = "questId";
    private static final String Q_TYPE        = "type";
    private static final String Q_TARGET      = "target";
    private static final String Q_PROGRESS    = "progress";
    private static final String Q_START_TIME  = "startTime";
    private static final String Q_EXPIRATION  = "expiration";
    private static final String Q_CLAIM_WIN   = "claimWindow";
    private static final String Q_TARGET_ITEM = "targetItem";

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public WeeklyBundleStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quests-bundles.yml");
        reload();
    }

    public synchronized void reload() {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("[Quests] Could not create data folder: " + parent);
            }
            try {
                if (!file.exists() && !file.createNewFile()) {
                    plugin.getLogger().warning("[Quests] Could not create file: " + file);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Quests] Failed to create file: " + file, e);
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save(WeeklyBundle bundle) {
        String base = ROOT + "." + bundle.getBundleId();
        yaml.set(base + "." + OWNER, bundle.getOwner().toString());
        yaml.set(base + "." + EXPIRES, bundle.getExpiresAtEpochSeconds());
        yaml.set(base + "." + QUESTS, serializeQuests(bundle.getQuests()));

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Quests] Failed saving " + file + ": " + e.getMessage(), e);
        }
    }

    public synchronized WeeklyBundle load(UUID bundleId) {
        String base = ROOT + "." + bundleId;
        if (!yaml.isConfigurationSection(base)) return null;

        ConfigurationSection s = yaml.getConfigurationSection(base);
        if (s == null) return null;

        String ownerStr = s.getString(OWNER);
        if (ownerStr == null) return null;

        UUID owner;
        try {
            owner = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[Quests] Invalid owner UUID for bundle " + bundleId + ": " + ownerStr);
            return null;
        }

        long expires = s.getLong(EXPIRES);
        List<Quest> quests = deserializeQuests(s.getList(QUESTS));

        WeeklyBundle bundle = new WeeklyBundle();
        bundle.setBundleId(bundleId);
        bundle.setOwner(owner);
        bundle.setExpiresAtEpochSeconds(expires);
        bundle.setQuests(quests);
        return bundle;
    }

    @SuppressWarnings("unused") // handy for tooling
    public synchronized void deleteBundle(UUID bundleId) {
        yaml.set(ROOT + "." + bundleId, null);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Quests] Failed deleting bundle " + bundleId + ": " + e.getMessage(), e);
        }
    }

    /** Remove all bundles for a given owner. @return how many were removed */
    public synchronized int purgeOwner(UUID ownerId) {
        ConfigurationSection root = yaml.getConfigurationSection(ROOT);
        if (root == null) return 0;

        int removed = 0;
        for (String key : new ArrayList<>(root.getKeys(false))) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            String owner = s.getString(OWNER);
            if (ownerId.toString().equals(owner)) {
                yaml.set(ROOT + "." + key, null);
                removed++;
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Quests] Failed purging bundles for " + ownerId + ": " + e.getMessage(), e);
        }
        return removed;
    }

    public synchronized WeeklyBundle findActiveFor(UUID ownerId, long nowEpochSeconds) {
        ConfigurationSection root = yaml.getConfigurationSection(ROOT);
        if (root == null) return null;

        WeeklyBundle best = null;
        long bestExpires = Long.MAX_VALUE;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            if (!ownerId.toString().equals(s.getString(OWNER))) continue;

            long expires = s.getLong(EXPIRES);
            if (nowEpochSeconds < expires && expires < bestExpires) {
                try {
                    best = load(UUID.fromString(key));
                    bestExpires = expires;
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[Quests] Bad bundle id in storage: " + key);
                }
            }
        }
        return best;
    }

    /* =========================
       Serialization helpers
       ========================= */

    private List<Map<String, Object>> serializeQuests(List<Quest> quests) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (quests == null) return out;
        for (Quest q : quests) {
            out.add(serializeQuest(q));
        }
        return out;
    }

    private Map<String, Object> serializeQuest(Quest q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(Q_ID, q.getId().toString());
        m.put(Q_QUEST_ID, q.getQuestId());
        m.put(Q_TYPE, q.getType().name());
        m.put(Q_TARGET, q.getTarget());
        m.put(Q_PROGRESS, q.getProgress());
        m.put(Q_START_TIME, q.getStartTime());
        m.put(Q_EXPIRATION, q.getExpirationSeconds());
        m.put(Q_CLAIM_WIN, q.getClaimWindowSeconds());
        m.put(Q_TARGET_ITEM, q.getTargetItem());
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<Quest> deserializeQuests(List<?> rawList) {
        List<Quest> quests = new ArrayList<>();
        if (rawList == null) return quests;

        for (Object o : rawList) {
            if (!(o instanceof Map<?, ?> map)) continue;
            Quest q = deserializeQuest((Map<String, Object>) map);
            if (q != null) quests.add(q);
        }
        return quests;
    }

    private Quest deserializeQuest(Map<String, Object> m) {
        try {
            UUID id           = UUID.fromString(String.valueOf(m.get(Q_ID)));
            String questId    = (String) m.get(Q_QUEST_ID);
            QuestType type    = QuestType.valueOf(String.valueOf(m.get(Q_TYPE)));
            int target        = ((Number) m.get(Q_TARGET)).intValue();
            int progress      = ((Number) m.get(Q_PROGRESS)).intValue();
            long startTime    = ((Number) m.get(Q_START_TIME)).longValue();
            long expiration   = ((Number) m.get(Q_EXPIRATION)).longValue();
            long claimWindow  = ((Number) m.get(Q_CLAIM_WIN)).longValue();
            String targetItem = (m.get(Q_TARGET_ITEM) == null) ? null : String.valueOf(m.get(Q_TARGET_ITEM));

            return new Quest(id, questId, type, target, progress, startTime, expiration, claimWindow, targetItem);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[Quests] Failed to deserialize quest: " + m, ex);
            return null;
        }
    }
}
