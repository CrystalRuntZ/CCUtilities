package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BiomeListener implements Listener {

    // Track last biome per player to avoid re-processing tiny moves within the same biome
    private final Map<UUID, NamespacedKey> lastBiome = new HashMap<>();

    @EventHandler
    public void onBiomeChange(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        var to = event.getTo();
        var from = event.getFrom();
        if (from.getWorld() != to.getWorld()) return;

        // Ignore movement within the same block
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        var biomeKey = to.getBlock().getBiome().getKey(); // e.g. minecraft:flower_forest
        UUID uuid = player.getUniqueId();

        // Only react when biome actually changes
        var prev = lastBiome.put(uuid, biomeKey);
        if (prev != null && prev.equals(biomeKey)) return;

        // Normalize the "target item" format we compare against: KEY part only, uppercased.
        // Templates/weekly generator should set targetItem like "FLOWER_FOREST", "SAVANNA", etc.
        String biomeTarget = biomeKey.getKey().toUpperCase();

        // 1) Weekly bundle path (persists + auto-syncs lore; respects per-biome targets)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.DISCOVER_BIOME, biomeTarget, 1);
        if (handled) return;

        // 2) Fallback: legacy single-quest items
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.DISCOVER_BIOME && !quest.isComplete() && !quest.isExpired()) {
                String target = quest.getTargetItem(); // expected like "FLOWER_FOREST"
                if (target != null && target.equalsIgnoreCase(biomeTarget)) {
                    // since these are "find one biome" quests, just add 1 (they complete at 1/1)
                    quest.setProgress(Math.min(quest.getTarget(), quest.getProgress() + 1));

                    // Update the held single-quest item lore
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
