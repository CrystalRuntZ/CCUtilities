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

import java.util.*;

public class GlideListener implements Listener {

    // Keep fractional distance between events so sub-1.0 blocks aren't lost.
    private static final Map<UUID, Double> residualByPlayer = new HashMap<>();

    @EventHandler
    public void onGlide(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        if (!player.isGliding()) return; // only count when actually gliding (elytra)

        var to = event.getTo();
        var from = event.getFrom();
        if (from.getWorld() != to.getWorld()) return;

        // Distance this tick (3D, so vertical glides count too)
        double delta = from.toVector().distance(to.toVector());
        if (delta <= 0) return;

        // Accumulate fractional distance
        UUID id = player.getUniqueId();
        double sum = residualByPlayer.getOrDefault(id, 0.0) + delta;

        // Award only whole blocks to the quest system
        int wholeBlocks = (int) Math.floor(sum);
        if (wholeBlocks <= 0) {
            residualByPlayer.put(id, sum); // still under 1.0 total, keep accumulating
            return;
        }

        // Keep the fractional remainder for next time
        residualByPlayer.put(id, sum - wholeBlocks);

        // 1) Try the weekly/bundle progress path first
        boolean handled = QuestProgress.get().addProgress(player, QuestType.ELYTRA_GLIDE, wholeBlocks);
        if (handled) return;

        // 2) Fallback: update any matching single quest items the player has
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.ELYTRA_GLIDE && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + wholeBlocks);

                // Update lore on matching quest item(s)
                var inv = player.getInventory();
                for (var item : inv.getContents()) {
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
