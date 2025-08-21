package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class SmeltListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onExtract(FurnaceExtractEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        String output = event.getItemType().name().toUpperCase(); // e.g., IRON_INGOT, COOKED_BEEF
        int amount = Math.max(0, event.getItemAmount());
        if (amount == 0) return;

        // 1) Weekly bundle first (persists + auto-syncs lore; respects target output if set)
        boolean handled = QuestProgress.get().addProgress(player, QuestType.SMELT_ITEMS, output, amount);
        if (handled) return;

        // 2) Fallback: legacy single-quest items (with flexible target matching)
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.SMELT_ITEMS || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be null (any smelt) or specific (e.g., IRON_INGOT)
            if (target == null || equalsNormalizedFlexible(target, output)) {
                quest.setProgress(Math.min(quest.getTarget(), quest.getProgress() + amount));

                // Sync any corresponding quest item in the player's inventory
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

        // simple trailing 's' (ingots -> ingot)
        if (a.endsWith("s") && a.length() > 1 && baseMinus(a, 1).equals(b)) return true;

        // trailing 'es' (potatoes -> potato; cookies -> cookie)
        if (a.endsWith("es") && a.length() > 2 && baseMinus(a, 2).equals(b)) return true;

        // 'ies' -> 'ie' or 'y' (not common for smelts, but harmless)
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // also allow pluralizing toward b
        return (a + "s").equals(b) || (a + "es").equals(b);
    }

    private String baseMinus(String s, int suffixLen) {
        return s.substring(0, s.length() - suffixLen);
    }

    // Lowercase, strip namespace, normalize separators
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        int colon = s.indexOf(':');              // e.g., "minecraft:iron_ingot"
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }

        return s.replace('-', '_').replace(' ', '_');
    }
}
