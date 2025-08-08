package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestStorage;

import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        var player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<Quest> weeklyQuests = QuestStorage.loadWeeklyQuests(uuid);
        for (Quest quest : weeklyQuests) {
            if (!quest.isExpired()) {
                QuestManager.addQuest(player, quest);
            }
        }
    }
}
