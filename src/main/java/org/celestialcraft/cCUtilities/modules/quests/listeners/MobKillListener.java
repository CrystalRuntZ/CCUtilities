package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class MobKillListener implements Listener {

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var killer = event.getEntity().getKiller();
        if (killer == null) return;

        String killedType = event.getEntity().getType().name().toUpperCase();
        List<Quest> quests = QuestManager.getQuests(killer);

        for (Quest quest : quests) {
            if (quest.getType() == QuestType.KILL_MOBS && !quest.isComplete() && !quest.isExpired()) {
                String target = quest.getTargetItem();
                if (target == null || killedType.equalsIgnoreCase(target)) {
                    quest.setProgress(quest.getProgress() + 1);

                    for (var item : killer.getInventory().getContents()) {
                        if (item == null) continue;
                        var questId = LoreUtils.getQuestId(item);
                        if (questId != null && questId.equals(quest.getId())) {
                            LoreUtils.updateLore(item, quest);
                        }
                    }

                    LoreUtils.sendProgressActionBar(killer, quest);
                }
            }
        }
    }
}
