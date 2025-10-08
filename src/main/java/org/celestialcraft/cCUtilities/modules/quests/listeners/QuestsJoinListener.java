package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundle;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundleStorage;
import org.celestialcraft.cCUtilities.modules.quests.util.WeeklyQuestItemFactory;

import java.time.Instant;
import java.util.UUID;

public class QuestsJoinListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        var plugin = CCUtilities.getInstance();
        var storage = new WeeklyBundleStorage(plugin);
        long now = Instant.now().getEpochSecond();

        var inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            var it = inv.getItem(i);
            UUID id = WeeklyQuestItemFactory.getBundleId(plugin, it);
            if (id == null) continue;
            WeeklyBundle bundle = storage.load(id);
            if (bundle == null || bundle.isExpired(now)) continue;
            inv.setItem(i, WeeklyQuestItemFactory.syncLore(plugin, it, bundle));
        }
    }
}
