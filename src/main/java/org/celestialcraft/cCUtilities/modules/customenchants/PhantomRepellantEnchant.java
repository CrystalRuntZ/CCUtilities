package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class PhantomRepellantEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Phantom Repellant";

    @Override public String getIdentifier() { return "phantom_repellant"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() { return true; }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) { return applyTo(item, false); }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Phantom)) return;
        if (!(event.getTarget() instanceof Player p)) return;

        if (ItemChecks.hasAnywhere(p, this::hasEnchant)) {
            event.setCancelled(true);
        }
    }
}
