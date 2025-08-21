package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.List;

public class DamageListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        // We care about damage to any living entity (players, mobs, bosses), but NOT armor stands.
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof ArmorStand) return;

        // Find the responsible player (melee, projectile shooter, or tamed pet owner)
        Player attacker = resolveAttackingPlayer(event.getDamager());
        if (attacker == null) return;

        // Avoid self-damage cheese (e.g., harming yourself)
        if (victim instanceof Player && victim.getUniqueId().equals(attacker.getUniqueId())) return;

        double finalDamage = event.getFinalDamage();
        if (finalDamage <= 0.0) return;

        int amount = Math.max(1, (int) Math.round(finalDamage));

        // 1) Weekly bundle first (persists + auto-syncs lore)
        boolean handled = QuestProgress.get().addProgress(attacker, QuestType.DAMAGE_MOBS, amount);
        if (handled) return;

        // 2) Fallback: single-quest item flow
        List<Quest> quests = QuestManager.getQuests(attacker);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.DAMAGE_MOBS && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + amount);

                attacker.getInventory().forEach(item -> {
                    if (item == null) return;
                    var questId = LoreUtils.getQuestId(item);
                    if (questId != null && questId.equals(quest.getId())) {
                        LoreUtils.updateLore(item, quest);
                    }
                });

                LoreUtils.sendProgressActionBar(attacker, quest);
            }
        }
    }

    private Player resolveAttackingPlayer(Entity damager) {
        // Direct melee
        if (damager instanceof Player p) return p;

        // Ranged: arrows/tridents/snowballs/etc.
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }

        // Tamed pets (e.g., wolves)
        if (damager instanceof Tameable tame && tame.getOwner() instanceof Player p) {
            return p;
        }

        return null;
    }
}
