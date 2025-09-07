package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;
import org.celestialcraft.cCUtilities.util.PotionHelpers;

public class InvisibilityEffectEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Invisibility Effect";

    @Override public String getIdentifier() { return "invisibility_effect"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // Allow on any non-air item so anvils/books can add the lore to anything
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

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* no direct combat effect */ }

    // Refresh invisibility “quietly” whenever we get common player events.
    @Override public void onHeld(PlayerItemHeldEvent event) { applyIfActive(event.getPlayer()); }
    @Override public void onHandSwap(Player player)         { applyIfActive(player); }
    @Override public void onPlayerMove(Player player)       { applyIfActive(player); }
    @Override public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) { applyIfActive(event.getPlayer()); }

    private void applyIfActive(Player p) {
        if (p == null || !p.isOnline()) return;
        if (!ItemChecks.hasAnywhere(p, this::hasEnchant)) return;
        // Hidden: ambient=true, particles=false, icon=false.
        PotionHelpers.addHiddenOrRefresh(p, PotionEffectType.INVISIBILITY, 0, 200);
    }
}
