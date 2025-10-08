package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.modules.customenchants.LevitationImmunityEnchant;

public class LevitationImmunityListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        // Only care about LEVITATION being added or changed
        final PotionEffectType type = event.getModifiedType();
        if (type != PotionEffectType.LEVITATION) return;

        switch (event.getAction()) {
            case ADDED, CHANGED -> {
                if (LevitationImmunityEnchant.isActiveFor(p)) {
                    // Prefer cancelling to stop it before it applies
                    event.setCancelled(true);
                    // And just in case, also strip any existing Levitation
                    p.removePotionEffect(PotionEffectType.LEVITATION);
                }
            }
            default -> { /* ignore REMOVED/CLEARED */ }
        }
    }

    // Safety net: if some plugin re-adds Levitation on the same tick after cancellation,
    // Paper sometimes ends up with a net-new effect; we can clear at MONITOR too.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterPotionApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        // If player ends up with LEVITATION and we should be immune, purge it.
        PotionEffect current = p.getPotionEffect(PotionEffectType.LEVITATION);
        if (current != null && LevitationImmunityEnchant.isActiveFor(p)) {
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }
}
