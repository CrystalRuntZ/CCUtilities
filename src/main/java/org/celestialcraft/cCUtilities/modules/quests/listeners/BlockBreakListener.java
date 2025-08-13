package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;
import org.celestialcraft.cCUtilities.modules.quests.util.WeeklyQuestItemFactory;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        Material type = event.getBlock().getType();
        if (type != Material.STONE && type != Material.COBBLESTONE) return;

        var player = event.getPlayer();

        boolean hasWeeklyPaper = false;
        var plugin = CCUtilities.getInstance();
        for (var it : player.getInventory().getContents()) {
            if (WeeklyQuestItemFactory.getBundleId(plugin, it) != null) {
                hasWeeklyPaper = true;
                break;
            }
        }

        if (hasWeeklyPaper) {
            QuestProgress.get().addProgress(player, QuestType.MINE_BLOCK, 1);
            return;
        }

        for (Quest quest : QuestManager.getQuests(player)) {
            if (quest.getType() == QuestType.MINE_BLOCK && !quest.isComplete() && !quest.isExpired()) {
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
