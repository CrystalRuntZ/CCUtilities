package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class UnbreakableEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Unbreakable";

    @Override public String getIdentifier() { return "unbreakable"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return isDamageable(item);
    }

    private boolean isDamageable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return isDamageable(item) && LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (!isDamageable(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (hasEnchant(item)) {
            event.setCancelled(true); // no durability loss
        }
    }
}
