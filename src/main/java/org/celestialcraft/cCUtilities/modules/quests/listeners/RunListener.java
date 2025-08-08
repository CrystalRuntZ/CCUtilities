package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class RunListener implements Listener {

    @EventHandler
    public void onRun(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var player = event.getPlayer();
        var from = event.getFrom();
        var to = event.getTo();
        if (from.getBlock().getLocation().equals(to.getBlock().getLocation())) return;

        var block = player.getLocation().getBlock();
        if (block.getType() != Material.WATER) {
            int distance = (int) from.distance(to);
            List<Quest> quests = QuestManager.getQuests(player);

            for (Quest quest : quests) {
                if (quest.getType() == QuestType.RUN_DISTANCE && !quest.isComplete() && !quest.isExpired()) {
                    quest.setProgress(quest.getProgress() + distance);

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
