package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class PlaceBlockListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var player = event.getPlayer();
        String placedType = event.getBlock().getType().name().toUpperCase();
        List<Quest> quests = QuestManager.getQuests(player);

        for (Quest quest : quests) {
            if (quest.getType() == QuestType.PLACE_BLOCKS && !quest.isComplete() && !quest.isExpired()) {
                String target = quest.getTargetItem();
                if (target == null || placedType.equalsIgnoreCase(target)) {
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
