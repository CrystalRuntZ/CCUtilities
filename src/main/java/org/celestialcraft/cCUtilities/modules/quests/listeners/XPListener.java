package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class XPListener implements Listener {

    @EventHandler
    public void onGainXP(PlayerExpChangeEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var player = event.getPlayer();
        int amount = event.getAmount();

        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.GAIN_EXPERIENCE && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + amount);

                for (var item : player.getInventory().getContents()) {
                    if (item == null) continue;
                    var questId = LoreUtils.getQuestId(item);
                    if (questId != null && questId.equals(quest.getId())) {
                        LoreUtils.updateLore(item, quest);
                    }
                }

                LoreUtils.sendProgressActionBar(player, quest);
            }
        }
    }
}
