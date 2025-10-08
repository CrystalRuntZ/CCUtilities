package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class DragonbreathImmunityEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Dragon Breath Immunity";

    @Override public String getIdentifier() { return "dragon_breath_immunity"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // Allow on anything that isn't air
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() {
        // Mark as truly any-item (paper, sugarcane, etc.)
        return true;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    // Back-compat path
    @Override
    public ItemStack applyTo(ItemStack item) {
        return applyTo(item, false);
    }

    // Force-apply path (anvil/merging will use this when allowed)
    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        // Write lore with italics disabled
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!ItemChecks.hasAnywhere(p, this::hasEnchant)) return;

        // Dragon breath damage comes via an AreaEffectCloud
        if (event.getDamager() instanceof AreaEffectCloud) {
            event.setCancelled(true);
        }
    }
}
