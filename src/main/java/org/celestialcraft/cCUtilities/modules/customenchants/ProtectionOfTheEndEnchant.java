package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class ProtectionOfTheEndEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Protection of the End";

    @Override public String getIdentifier() { return "protection_of_the_end"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String n = item.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
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

    /** Damage reduction in THE_END; scales by number of enchanted armor pieces. */
    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        int pieces = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) pieces++;
        }
        if (pieces <= 0) return;

        double mul = switch (pieces) {
            case 1 -> 0.75;  // 25% reduction
            case 2 -> 0.57;  // ~43%
            case 3 -> 0.43;  // ~57%
            case 4 -> 0.33;  // ~67%
            default -> 1.0;
        };
        event.setDamage(event.getDamage() * mul);
    }
}
