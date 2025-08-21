package org.celestialcraft.cCUtilities.modules.quests.listeners;

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

public class BlockBreakListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player   = event.getPlayer();
        var typeName = event.getBlock().getType().name(); // e.g. "GRANITE", "OAK_LOG", etc.

        // 1) Weekly bundle first (persists + auto-syncs lore; plural/variant tolerant in service)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.MINE_BLOCK, typeName, 1);
        if (handled) return;

        // 2) Fallback: legacy single-quest items
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.MINE_BLOCK || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be null (any block) or a specific material-like name
            if (target == null || equalsNormalizedFlexible(target, typeName)) {
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

    /* -------- plural/variant tolerant matching (mirrors QuestProgressService) -------- */

    private boolean equalsNormalizedFlexible(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return na.equals(nb) || matchesFlex(na, nb) || matchesFlex(nb, na);
    }

    private boolean matchesFlex(String a, String b) {
        if (a.equals(b)) return true;

        // simple trailing 's'  (granites -> granite, panes -> pane)
        if (a.endsWith("s") && a.length() > 1) {
            if (baseMinus(a, 1).equals(b)) return true;
        }

        // trailing 'es' (witches -> witch, foxes -> fox)
        if (a.endsWith("es") && a.length() > 2) {
            if (baseMinus(a, 2).equals(b)) return true;
        }

        // 'ies' -> 'ie' OR 'y' (zombies -> zombie)
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // 'ves' -> 'f' / 'fe' (wolves -> wolf)
        if (a.endsWith("ves") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "f").equals(b) || (base3 + "fe").equals(b)) return true;
        }

        // also allow pluralizing toward b (reverse handled by calling matchesFlex the other way)
        return (a + "s").equals(b) || (a + "es").equals(b);
    }

    private String baseMinus(String s, int suffixLen) {
        return s.substring(0, s.length() - suffixLen);
    }

    // normalize to lower_underscore and strip any namespace if present
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        int colon = s.indexOf(':');            // allow "minecraft:granite"
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }

        return s.replace('-', '_').replace(' ', '_');
    }
}
