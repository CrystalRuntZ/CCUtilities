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

import java.util.List;

public class BreedListener implements Listener {

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        if (!(event.getBreeder() instanceof Player breeder)) return;

        String bredType = event.getEntityType().name().toUpperCase();
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
