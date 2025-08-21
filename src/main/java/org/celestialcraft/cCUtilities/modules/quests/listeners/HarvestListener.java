package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class HarvestListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var block = event.getBlock();
        var data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return; // only count fully-grown crops

        var player = event.getPlayer();
        String cropType = block.getType().name().toUpperCase();  // e.g., WHEAT, POTATOES, CARROTS, NETHER_WART

        // 1) Weekly bundle first (persists + auto-syncs lore; respects target crop if specified)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.HARVEST_CROPS, cropType, 1);
        if (handled) return;

        // 2) Fallback: legacy single-quest items with flexible target matching
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.HARVEST_CROPS || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be null (any crop) or a specific material name
            if (target == null || equalsNormalizedFlexible(target, cropType)) {
                quest.setProgress(Math.min(quest.getTarget(), quest.getProgress() + 1));

                // Update any matching quest item in inventory
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

    /* ---------------- plural/variant-tolerant matching ---------------- */

    private boolean equalsNormalizedFlexible(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return na.equals(nb) || matchesFlex(na, nb) || matchesFlex(nb, na);
    }

    private boolean matchesFlex(String a, String b) {
        if (a.equals(b)) return true;

        // simple trailing 's' (carrots -> carrot)
        if (a.endsWith("s") && a.length() > 1 && baseMinus(a, 1).equals(b)) return true;

        // trailing 'es' (potatoes -> potato)
        if (a.endsWith("es") && a.length() > 2 && baseMinus(a, 2).equals(b)) return true;

        // 'ies' -> 'ie' or 'y' (berries -> berry)
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // allow pluralizing toward b
        return (a + "s").equals(b) || (a + "es").equals(b);
    }

    private String baseMinus(String s, int suffixLen) {
        return s.substring(0, s.length() - suffixLen);
    }

    // lowercase, strip namespace if present, normalize separators
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        int colon = s.indexOf(':'); // e.g., "minecraft:wheat"
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }
        return s.replace('-', '_').replace(' ', '_');
    }
}
