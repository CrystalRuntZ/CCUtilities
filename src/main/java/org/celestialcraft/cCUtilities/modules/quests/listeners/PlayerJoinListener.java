package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundle;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundleStorage;

import java.time.Instant;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        var storage = new WeeklyBundleStorage(CCUtilities.getInstance());
        long now = Instant.now().getEpochSecond();

        WeeklyBundle bundle = storage.findActiveFor(player.getUniqueId(), now);
        if (bundle == null || bundle.isExpired(now)) return;

        bundle.getQuests().forEach(q -> {
            if (!q.isExpired() && !QuestManager.hasQuestOfType(player, q.getType())) {
                QuestManager.addQuest(player, q);
            }
        });
    }
}
