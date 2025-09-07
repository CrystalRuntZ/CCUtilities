package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.EnumSet;
import java.util.Set;

public class ZombieRepellantEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Zombie Repellant";

    private static final Set<EntityType> ZOMBIE_TYPES = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.ZOMBIFIED_PIGLIN
    );

    @Override public String getIdentifier() { return "zombie_repellant"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // allow on any non-air item; lore-based
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() { return true; }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* no combat effect */ }

    @Override
    public ItemStack applyTo(ItemStack item) { return applyTo(item, false); }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!ZOMBIE_TYPES.contains(event.getEntityType())) return;
        if (!(event.getTarget() instanceof Player p)) return;

        if (ItemChecks.hasAnywhere(p, this::hasEnchant)) {
            event.setCancelled(true);
        }
    }
}
