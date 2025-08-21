package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class BreedListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;
        if (!(event.getBreeder() instanceof Player breeder)) return;

        String bredType = event.getEntityType().name().toUpperCase(); // e.g. "COW", "FOX", "MUSHROOM_COW"

        // 1) Weekly bundle first (persists + auto-syncs lore; flexible matching in service)
        boolean handled = QuestProgress.get().addProgress(breeder, QuestType.BREED_ANIMALS, bredType, 1);
        if (handled) return;

        // 2) Fallback: legacy single-quest items with flexible target matching
        List<Quest> quests = QuestManager.getQuests(breeder);
        for (Quest quest : quests) {
            if (quest.getType() != QuestType.BREED_ANIMALS || quest.isComplete() || quest.isExpired()) continue;

            String target = quest.getTargetItem(); // may be null (any animal) or a specific mob name
            if (target == null || equalsNormalizedFlexible(target, bredType)) {
                quest.setProgress(Math.min(quest.getTarget(), quest.getProgress() + 1));

                // Update any matching quest item in inventory
                breeder.getInventory().forEach(item -> {
                    if (item == null) return;
                    var questId = LoreUtils.getQuestId(item);
                    if (questId != null && questId.equals(quest.getId())) {
                        LoreUtils.updateLore(item, quest);
                    }
                });

                LoreUtils.sendProgressActionBar(breeder, quest);
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

        // simple trailing 's'  (cows -> cow)
        if (a.endsWith("s") && a.length() > 1) {
            if (baseMinus(a, 1).equals(b)) return true;
        }

        // trailing 'es' (foxes -> fox)
        if (a.endsWith("es") && a.length() > 2) {
            if (baseMinus(a, 2).equals(b)) return true;
        }

        // 'ies' -> 'ie' OR 'y' (bunnies -> bunny, zombies -> zombie)
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // 'ves' -> 'f' / 'fe' (wolves -> wolf)
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

    // normalize to lower_underscore and strip any namespace if present
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        int colon = s.indexOf(':'); // allow "minecraft:cow"
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }

        return s.replace('-', '_').replace(' ', '_');
    }
}
