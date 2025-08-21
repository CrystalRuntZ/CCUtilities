package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class BiomeListener implements Listener {

    // --- Move across a biome border ---
    @EventHandler(ignoreCancelled = true)
    public void onBiomeChange(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        event.getTo();
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;

        NamespacedKey fromKey = event.getFrom().getBlock().getBiome().getKey();
        NamespacedKey toKey   = event.getTo().getBlock().getBiome().getKey();

        // Only act when the biome actually changes
        if (fromKey.equals(toKey)) return;

        handleEnterBiome(event.getPlayer(), toKey);
    }

    // --- Teleport into a biome (even if not moving across a border) ---
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        event.getTo();
        handleEnterBiome(event.getPlayer(), event.getTo().getBlock().getBiome().getKey());
    }

    // --- Change worlds: count the destination biome immediately ---
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        Player p = event.getPlayer();
        handleEnterBiome(p, p.getLocation().getBlock().getBiome().getKey());
    }

    private void handleEnterBiome(Player player, NamespacedKey biomeKey) {
        // Weekly bundle path: compare against key part only (e.g., "FLOWER_FOREST")
        String biomeTarget = biomeKey.getKey().toUpperCase();
        boolean handled = QuestProgress.get().addProgress(player, QuestType.DISCOVER_BIOME, biomeTarget, 1);
        if (handled) return;

        // Fallback: legacy single-quest item flow
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.DISCOVER_BIOME || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be "FLOWER_FOREST" or "minecraft:flower_forest"
            if (target == null) continue;

            if (normalizeBiomeName(target).equals(normalizeBiomeName(biomeKey.toString()))) {
                quest.setProgress(Math.min(quest.getTarget(), quest.getProgress() + 1));

                // Update any matching legacy quest item in inventory
                player.getInventory().forEach(item -> {
                    if (item == null) return;
                    var questId = LoreUtils.getQuestId(item);
                    if (questId != null && questId.equals(quest.getId())) {
                        LoreUtils.updateLore(item, quest);
                    }
                });

                LoreUtils.sendProgressActionBar(player, quest);
            }
        }
    }

    private String normalizeBiomeName(String s) {
        if (s == null) return "";
        // Drop namespace if present, lowercase, replace spaces with underscores
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        return s.trim().toLowerCase().replace(' ', '_');
    }
}
