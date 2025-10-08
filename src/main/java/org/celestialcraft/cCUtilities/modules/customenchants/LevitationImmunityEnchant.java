package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.util.EnchantUtil;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.Arrays;

public class LevitationImmunityEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Levitation Immunity";
    private static final String IDENT    = "levitation_immunity";

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

    // Central listener handles equip/unequip detection; keep hooks as no-ops,
    // but we also opportunistically clear Levitation if the player starts holding it.
    @Override public void onHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if (LevitationImmunityEnchant.isActiveFor(p)) {
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    @Override public void onHandSwap(Player player) {
        if (LevitationImmunityEnchant.isActiveFor(player)) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    @Override public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (LevitationImmunityEnchant.isActiveFor(p)) {
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    @Override public void onQuit(PlayerQuitEvent event) { /* no-op */ }

    /**
     * Returns true if the player has the Levitation Immunity enchant "active".
     * Here we treat "active" as present on main hand, off hand, or any worn armor.
     * (Adjust this if your project uses a different rule.)
     */
    public static boolean isActiveFor(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off  = p.getInventory().getItemInOffHand();

        if (hasEnchantStatic(main) || hasEnchantStatic(off)) return true;

        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(LevitationImmunityEnchant::hasEnchantStatic);
    }

    private static boolean hasEnchantStatic(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return EnchantUtil.hasTag(item, IDENT) || LoreUtil.itemHasLore(item, RAW_LORE);
    }
}
