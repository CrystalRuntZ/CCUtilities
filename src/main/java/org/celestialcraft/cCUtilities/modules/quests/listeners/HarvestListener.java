package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class HarvestListener implements Listener {

    @EventHandler
    public void onHarvest(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var block = event.getBlock();
        var data = block.getBlockData();

        if (data instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
            var player = event.getPlayer();
            var blockType = block.getType().name().toUpperCase();
            List<Quest> quests = QuestManager.getQuests(player);

            for (Quest quest : quests) {
                if (quest.getType() == QuestType.HARVEST_CROPS && !quest.isComplete() && !quest.isExpired()) {
                    String target = quest.getTargetItem();
                    if (target == null || target.equalsIgnoreCase(blockType)) {
                        quest.setProgress(quest.getProgress() + 1);

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
    }
}
