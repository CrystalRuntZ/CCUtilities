package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BleedEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Bleed Effect";

    private static final double DAMAGE_MULTIPLIER   = 1.2;  // +20% while bleeding
    private static final int    BLEED_DURATION_TICKS = 100; // 5s
    private static final double BLEED_CHANCE        = 0.20; // 20%

    private final Set<UUID> bleedingEntities = new HashSet<>();

    @Override public String getIdentifier() { return "bleed_effect"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!hasEnchant(weapon)) return;

        UUID targetId = target.getUniqueId();

        // Extra damage while bleeding
        if (bleedingEntities.contains(targetId)) {
            event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);
        }

        // Chance to start bleeding
        if (!bleedingEntities.contains(targetId) && Math.random() < BLEED_CHANCE) {
            bleedingEntities.add(targetId);
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10);

            // Clear bleeding after duration
            Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> bleedingEntities.remove(targetId), BLEED_DURATION_TICKS);
        }
    }
}
