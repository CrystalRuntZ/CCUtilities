package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class DestroyerOfTheEndEnchant implements CustomEnchant {
    private static final String RAW_LORE  = "&7Destroyer of the End";

    @Override public String getIdentifier() { return "destroyer_of_the_end"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return switch (type) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 WOODEN_AXE,   STONE_AXE,   IRON_AXE,   GOLDEN_AXE,   DIAMOND_AXE,   NETHERITE_AXE,
                 BOW, CROSSBOW, TRIDENT, MACE -> true;
            default -> false;
        };
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        // Insert/move the lore line right after the tag/enchant block (non-italic, de-duped)
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof LivingEntity living)) return;

        ItemStack weapon = (living.getEquipment() != null) ? living.getEquipment().getItemInMainHand() : null;
        if (!hasEnchant(weapon)) return;

        Entity target = event.getEntity();
        if (target.getWorld().getEnvironment() != World.Environment.THE_END) return;

        if (isOnMainIsland(target)) {
            event.setDamage(event.getDamage() * 1.5);
        }
    }

    private boolean isOnMainIsland(Entity entity) {
        // Adjust radius to taste; assumes End spawn is on the main island.
        return entity.getLocation().distanceSquared(entity.getWorld().getSpawnLocation()) <= 500 * 500;
    }
}
