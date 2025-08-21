package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class MobKillListener implements Listener {

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Block boss kills from counting
        EntityType type = event.getEntityType();
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON) return;

        // Prefer namespaced key path (lowercase with underscores), e.g. "husk", "zombie_villager"
        String killedKey = type.getKey().getKey(); // no "minecraft:" prefix

        // 1) Weekly bundle first (persists + auto-syncs lore; respects target mob)
        boolean handled = QuestProgress.get().addProgress(killer, QuestType.KILL_MOBS, killedKey, 1);
        if (handled) return;

        // 2) Fallback: single-quest item flow (legacy)
        List<Quest> quests = QuestManager.getQuests(killer);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.KILL_MOBS && !quest.isComplete() && !quest.isExpired()) {
                String target = quest.getTargetItem(); // might be "zombies", "minecraft:husk", "HUSK", etc.
                if (target == null || equalsNormalized(target, killedKey)) {
                    quest.setProgress(quest.getProgress() + 1);

                    killer.getInventory().forEach(item -> {
                        if (item == null) return;
                        var questId = LoreUtils.getQuestId(item);
                        if (questId != null && questId.equals(quest.getId())) {
                            LoreUtils.updateLore(item, quest);
                        }
                    });

                    LoreUtils.sendProgressActionBar(killer, quest);
                }
            }
        }
    }

    /* -----------------------
       Same flexible matching as service
       ----------------------- */

    private boolean equalsNormalized(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb)) return true;
        return matchesFlex(na, nb) || matchesFlex(nb, na);
    }

    private boolean matchesFlex(String a, String b) {
        if (stripSuffix(a, "s").equals(b)) return true;
        if (stripSuffix(a, "es").equals(b)) return true;

        if (a.endsWith("ies") && a.length() > 3) {
            String x = a.substring(0, a.length() - 3) + "ie"; // zombies -> zombie
            if (x.equals(b)) return true;
        }

        if (a.endsWith("ves") && a.length() > 3) {
            String x1 = a.substring(0, a.length() - 3) + "f";  // wolves -> wolf
            String x2 = a.substring(0, a.length() - 3) + "fe"; // knives -> knife (not a mob, but harmless)
            if (x1.equals(b) || x2.equals(b)) return true;
        }

        if ((a + "s").equals(b)) return true;
        if ((a + "es").equals(b)) return true;

        return false;
    }

    private String stripSuffix(String s, String suffix) {
        return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
    }

    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();
        int colon = s.indexOf(':');
        if (colon >= 0 && colon < s.length() - 1) {
            s = s.substring(colon + 1);
        }
        s = s.replace('-', '_').replace(' ', '_');
        return s;
    }
}
