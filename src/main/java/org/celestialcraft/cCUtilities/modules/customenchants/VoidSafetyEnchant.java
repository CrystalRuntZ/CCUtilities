package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.util.EnchantUtil;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.Arrays;

public class VoidSafetyEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Void Safety";
    private static final String IDENT    = "void_safety";

    @Override public String getIdentifier() { return IDENT; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    // Applicable to ANY item, including non-enchantables
    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return EnchantUtil.hasTag(item, getIdentifier()) || LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item) || hasEnchant(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        EnchantUtil.setTag(item, getIdentifier());
        return item;
    }

    // Central listener handles the actual teleport; hooks kept as no-ops
    @Override public void onHeld(PlayerItemHeldEvent event) { /* no-op */ }
    @Override public void onHandSwap(Player player)         { /* no-op */ }
    @Override public void onJoin(PlayerJoinEvent event)     { /* no-op */ }
    @Override public void onQuit(PlayerQuitEvent event)     { /* no-op */ }

    /** Active if on main hand, off hand, or any worn armor. */
    public static boolean isActiveFor(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off  = p.getInventory().getItemInOffHand();
        if (hasEnchantStatic(main) || hasEnchantStatic(off)) return true;
        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(VoidSafetyEnchant::hasEnchantStatic);
    }

    private static boolean hasEnchantStatic(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return EnchantUtil.hasTag(item, IDENT) || LoreUtil.itemHasLore(item, RAW_LORE);
    }
}
