package org.celestialcraft.cCUtilities.modules.quests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.modules.quests.config.QuestConfig;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestStorage;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestItemFactory;

import java.util.*;

public class QuestManager {

    private static final Map<UUID, List<Quest>> activeQuests = new HashMap<>();

    public static List<Quest> getQuests(Player player) {
        return activeQuests.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public static void addQuest(Player player, Quest quest) {
        UUID uuid = player.getUniqueId();
        activeQuests.computeIfAbsent(uuid, k -> new ArrayList<>()).add(quest);
    }

    public static void removeQuest(Player player, Quest quest) {
        List<Quest> quests = activeQuests.get(player.getUniqueId());
        if (quests != null) {
            quests.remove(quest);
        }
    }

    public static boolean hasQuestOfType(Player player, QuestType type) {
        return getQuests(player).stream()
                .anyMatch(q -> q.getType() == type && !q.isExpired());
    }

    public static void updateProgress(Player player, QuestType type, int amount) {
        List<Quest> quests = activeQuests.get(player.getUniqueId());
        if (quests == null) return;

        for (Quest quest : quests) {
            if (quest.getType() == type && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + amount);
                updateLoreForQuestItem(player, quest);
            }
        }
    }

    private static void updateLoreForQuestItem(Player player, Quest quest) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            var item = player.getInventory().getItem(slot);
            if (item == null) continue;
            if (LoreUtils.isQuestItem(item)) {
                LoreUtils.updateLore(item, quest);
            }
        }
    }

    public static List<Quest> claimCompletedQuests(Player player) {
        List<Quest> claimed = new ArrayList<>();
        List<Quest> quests = activeQuests.get(player.getUniqueId());
        if (quests == null) return claimed;

        Iterator<Quest> iterator = quests.iterator();
        while (iterator.hasNext()) {
            Quest quest = iterator.next();
            if (quest.isComplete() && quest.canClaim()) {
                var template = QuestConfig.getTemplate(quest.getQuestId());
                if (template == null) continue;

                for (String raw : template.rewardCommands()) {
                    String command = raw.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }

                player.sendMessage("§6Reward granted for completing: " + template.display());
                iterator.remove();
                claimed.add(quest);

                advanceQuestChain(player, quest.getQuestId());
            }
        }

        return claimed;
    }

    private static void advanceQuestChain(Player player, String completedId) {
        for (Map.Entry<String, List<String>> entry : QuestConfig.getAllChains().entrySet()) {
            List<String> questIds = entry.getValue();
            int index = questIds.indexOf(completedId);
            if (index != -1 && index + 1 < questIds.size()) {
                String nextId = questIds.get(index + 1);
                var template = QuestConfig.getTemplate(nextId);
                if (template == null) continue;

                Quest nextQuest = new Quest(
                        UUID.randomUUID(),
                        nextId,
                        template.type(),
                        template.target(),
                        template.expiration(),
                        template.claimWindow(),
                        template.targetItem()
                );

                var item = QuestItemFactory.createQuestItem(nextQuest);
                player.getInventory().addItem(item);
                addQuest(player, nextQuest);

                player.sendMessage("§eYou unlocked the next quest: " + template.display());
            }
        }
    }

    public static void saveWeeklyQuests(UUID playerId, List<Quest> quests) {
        QuestStorage.saveWeeklyQuests(playerId, quests);
    }


    public static void clearAllQuests(Player player) {
        activeQuests.remove(player.getUniqueId());
    }

    public static Map<UUID, List<Quest>> getAll() {
        return activeQuests;
    }

    public static void set(UUID uuid, List<Quest> quests) {
        activeQuests.put(uuid, quests);
    }
}
