package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.util.LoreUtil;
import org.celestialcraft.cCUtilities.util.PotionHelpers;

public class JumpBoostTwoEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Jump Boost II";

    @Override public String getIdentifier() { return "jump_boost_two"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() {
        return true;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return applyTo(item, false);
    }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override public void onHeld(PlayerItemHeldEvent event) { applyIfActive(event.getPlayer()); }
    @Override public void onHandSwap(Player player)         { applyIfActive(player); }
    @Override public void onPlayerMove(Player player)       { applyIfActive(player); }

    private void applyIfActive(Player p) {
        if (p == null || !p.isOnline()) return;
        boolean active = org.celestialcraft.cCUtilities.util.ItemChecks.hasAnywhere(p, this::hasEnchant);
        if (!active) return;

        // Jump Boost II (amplifier 1), hidden, ~11s
        PotionHelpers.addHiddenOrRefresh(p, PotionEffectType.JUMP_BOOST, 1, 220);
    }
}
