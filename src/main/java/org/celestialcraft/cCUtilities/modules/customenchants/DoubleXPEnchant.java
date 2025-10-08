package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class DoubleXPEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Double XP";

    // Metadata keys to ensure we only double once per event
    private static final String META_ENTITY_DBL = "ccu_double_xp_entity_done";
    private static final String META_BLOCK_DBL  = "ccu_double_xp_block_done";

    @Override public String getIdentifier() { return "double_xp"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        // Ensure lore is present and NON-ITALIC via LoreUtil
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();

        // Guard: only double once per dead entity
        if (dead.hasMetadata(META_ENTITY_DBL)) return;

        if (event.getEntity().getKiller() == null) return;
        ItemStack weapon = event.getEntity().getKiller().getInventory().getItemInMainHand();
        if (!hasEnchant(weapon)) return;

        int xp = event.getDroppedExp();
        if (xp <= 0) return;

        event.setDroppedExp(xp * 2);
        dead.setMetadata(META_ENTITY_DBL, new FixedMetadataValue(CCUtilities.getInstance(), Boolean.TRUE));
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Guard: only double once per broken block
        if (block.hasMetadata(META_BLOCK_DBL)) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!hasEnchant(tool)) return;

        int xp = event.getExpToDrop();
        if (xp <= 0) return;

        event.setExpToDrop(xp * 2);
        block.setMetadata(META_BLOCK_DBL, new FixedMetadataValue(CCUtilities.getInstance(), Boolean.TRUE));
    }
}
