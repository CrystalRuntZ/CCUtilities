package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class EndermanPleaserEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Enderman Pleaser";

    @Override public String getIdentifier() { return "enderman_pleaser"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    /** Applies to ANY item, including normally non-enchantable ones. */
    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    /** Add the lore line if missing and return the same instance (mirrors your Autosmelt pattern). */
    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null) return null;
        if (!appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    /** No combat effect; protection is handled at targeting time. */
    @Override
    public void applyEffect(org.bukkit.event.entity.EntityDamageByEntityEvent event) { /* no-op */ }

    /** Core behavior: prevent Endermen from targeting players holding/wearing an item with this enchant. */
    @Override
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Enderman)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        if (hasEnchantAnywhere(player)) {
            // Stop the aggro; nulling the target + cancel covers different server versions/reasons.
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    /* ----------------- helpers ----------------- */

    /** Check main hand, offhand, and all armor slots for the enchant. */
    private boolean hasEnchantAnywhere(Player player) {
        PlayerInventory inv = player.getInventory();

        // hands
        if (hasEnchant(inv.getItemInMainHand())) return true;
        if (hasEnchant(inv.getItemInOffHand()))  return true;

        // armor (helmet, chest, legs, boots)
        for (ItemStack armor : new ItemStack[] {
                inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()
        }) {
            if (hasEnchant(armor)) return true;
        }

        return false;
    }
}
