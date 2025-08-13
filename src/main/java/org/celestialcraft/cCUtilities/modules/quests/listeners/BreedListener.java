package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class BreedListener implements Listener {

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        if (!(event.getBreeder() instanceof Player breeder)) return;

        String bredType = event.getEntityType().name().toUpperCase();

        // 1) Try weekly bundle first (persists + auto-syncs lore; respects target filter)
        boolean handled = QuestProgress.get().addProgress(breeder, QuestType.BREED_ANIMALS, bredType, 1);
        if (handled) return;

        // 2) Fallback: single-quest item flow (your original logic)
        List<Quest> quests = QuestManager.getQuests(breeder);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.BREED_ANIMALS && !quest.isComplete() && !quest.isExpired()) {
                String target = quest.getTargetItem();
                if (target == null || target.equalsIgnoreCase(bredType)) {
                    quest.setProgress(quest.getProgress() + 1);

                    for (var item : breeder.getInventory().getContents()) {
                        if (item == null) continue;
                        var questId = LoreUtils.getQuestId(item);
                        if (questId != null && questId.equals(quest.getId())) {
                            LoreUtils.updateLore(item, quest);
                        }
                    }

                    LoreUtils.sendProgressActionBar(breeder, quest);
                }
            }
        }
    }
}
