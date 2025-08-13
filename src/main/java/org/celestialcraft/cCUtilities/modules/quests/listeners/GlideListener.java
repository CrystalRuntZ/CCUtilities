package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class GlideListener implements Listener {

    @EventHandler
    public void onGlide(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        if (!player.isGliding()) return;

        var from = event.getFrom();
        var to = event.getTo();
        if (from.getWorld() != to.getWorld()) return;

        // Avoid spamming tiny moves within the same block
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        int distance = (int) Math.max(0, from.distance(to));
        if (distance <= 0) return;

        // 1) Try weekly bundle first (persists + auto-syncs lore)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.ELYTRA_GLIDE, distance);
        if (handled) return;

        // 2) Fallback: single-quest item flow (original logic)
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.ELYTRA_GLIDE && !quest.isComplete() && !quest.isExpired()) {
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
