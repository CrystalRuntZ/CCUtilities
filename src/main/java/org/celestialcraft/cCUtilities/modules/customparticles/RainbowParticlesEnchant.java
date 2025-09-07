package org.celestialcraft.cCUtilities.modules.customparticles;

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

public class RainbowParticlesEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Rainbow Spiral Particles";

    @Override public String getIdentifier() { return "rainbow_spiral_particles"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override public boolean appliesTo(ItemStack item) { return item != null && item.getType() != Material.AIR; }

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

    // Central listener handles equip/unequip detection; keep hooks as no-ops
    @Override public void onHeld(PlayerItemHeldEvent event) { /* no-op */ }
    @Override public void onHandSwap(Player player)         { /* no-op */ }
    @Override public void onJoin(PlayerJoinEvent event)      { /* no-op */ }
    @Override public void onQuit(PlayerQuitEvent event)      { /* no-op */ }
}
