package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SatietyEffectEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Satiety Effect";

    private static final long TOP_OFF_INTERVAL_MS = 1500L;
    private final Map<UUID, Long> lastTopOff = new HashMap<>();

    @Override public String getIdentifier() { return "satiety_effect"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() { return true; }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) { return applyTo(item, false); }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* no-op */ }

    @Override public void onHeld(PlayerItemHeldEvent event) { topOffIfActive(event.getPlayer()); }
    @Override public void onHandSwap(Player player)         { topOffIfActive(player); }
    @Override public void onPlayerMove(Player player)       { topOffIfActive(player); }

    private void topOffIfActive(Player p) {
        if (p == null || !p.isOnline()) return;
        if (!ItemChecks.hasAnywhere(p, this::hasEnchant)) return;

        long now = System.currentTimeMillis();
        Long last = lastTopOff.get(p.getUniqueId());
        if (last != null && now - last < TOP_OFF_INTERVAL_MS) return;
        lastTopOff.put(p.getUniqueId(), now);

        if (p.getFoodLevel() < 20) p.setFoodLevel(20);

        float maxSat = Math.min(20f, p.getFoodLevel());
        if (p.getSaturation() < maxSat) p.setSaturation(maxSat);

        if (p.getExhaustion() > 0f) p.setExhaustion(0f);
    }
}
