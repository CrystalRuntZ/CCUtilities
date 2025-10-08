package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class NightVisionEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Night Vision";

    @Override public String getIdentifier() { return "night_vision"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // any non-AIR item (paper, sugar cane, etc.)
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

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

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

    // Keep night vision “always on” while any enchanted item is active
    @Override public void onHeld(PlayerItemHeldEvent event) { applyIfActive(event.getPlayer()); }
    @Override public void onHandSwap(Player player)         { applyIfActive(player); }
    @Override public void onPlayerMove(Player player)       { applyIfActive(player); }
    @Override public void onJoin(PlayerJoinEvent event)     { applyIfActive(event.getPlayer()); }

    private void applyIfActive(Player p) {
        if (hasAnywhere(p)) {
            // short refresh so it appears permanent, no particles, no icon
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, false));
        }
    }

    private boolean hasAnywhere(Player p) {
        if (p == null || !p.isOnline()) return false;
        if (hasEnchant(p.getInventory().getItemInMainHand())) return true;
        if (hasEnchant(p.getInventory().getItemInOffHand()))  return true;
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) return true;
        }
        return false;
    }
}
