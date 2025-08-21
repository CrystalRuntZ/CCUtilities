package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class PlaceBlockListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        String placedType = event.getBlock().getType().name().toUpperCase(); // e.g. "GRANITE", "OAK_LOG"

        // 1) Weekly bundle first (persists + auto-syncs lore; respects target material if set)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.PLACE_BLOCKS, placedType, 1);
        if (handled) return;

        // 2) Fallback: legacy single-quest items with flexible target matching
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.PLACE_BLOCKS || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be null (any block) or a specific material name
            if (target == null || equalsNormalizedFlexible(target, placedType)) {
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

    /* -------- plural/variant tolerant matching (same pattern used elsewhere) -------- */

    private boolean equalsNormalizedFlexible(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return na.equals(nb) || matchesFlex(na, nb) || matchesFlex(nb, na);
    }

    private boolean matchesFlex(String a, String b) {
        if (a.equals(b)) return true;

        // simple trailing 's'  (granites -> granite)
        if (a.endsWith("s") && a.length() > 1) {
            if (baseMinus(a, 1).equals(b)) return true;
        }

        // trailing 'es' (arches -> arch; tiles -> tile)
        if (a.endsWith("es") && a.length() > 2) {
            if (baseMinus(a, 2).equals(b)) return true;
        }

        // 'ies' -> 'ie' or 'y' (bodies -> body; (rare for blocks but harmless))
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // 'ves' -> 'f' / 'fe' (leaves -> leaf)
        if (a.endsWith("ves") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "f").equals(b) || (base3 + "fe").equals(b)) return true;
        }

        // also allow pluralizing toward b
        return (a + "s").equals(b) || (a + "es").equals(b);
    }

    private String baseMinus(String s, int suffixLen) {
        return s.substring(0, s.length() - suffixLen);
    }

    // Lowercase, remove namespace, normalize separators.
    // Works well with block material names like "granite", "oak_log", "deepslate_tiles".
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        // strip namespace if present (e.g., "minecraft:granite" -> "granite")
        int colon = s.indexOf(':');
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }

        // unify separators to underscore
        return s.replace('-', '_').replace(' ', '_');
    }
}
