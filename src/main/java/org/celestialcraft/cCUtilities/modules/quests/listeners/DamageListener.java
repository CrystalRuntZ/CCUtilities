package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.List;

public class DamageListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int damage = (int) event.getDamage();
        List<Quest> quests = QuestManager.getQuests(player);

        for (Quest quest : quests) {
            if (quest.getType() == QuestType.DAMAGE_MOBS && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + damage);

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
