package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.ArrayList;
import java.util.List;

public class GreenThumbEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Doubled Harvest";
    // Guard key to prevent double-processing the same break
    private static final String META_BLOCK_DBL = "ccu_green_thumb_done";

    @Override public String getIdentifier() { return "green_thumb"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType().name().endsWith("_HOE");
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

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Guard: only run once per block break
        if (block.hasMetadata(META_BLOCK_DBL)) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!hasEnchant(tool)) return;

        if (!(block.getBlockData() instanceof Ageable age)) return;
        if (age.getAge() < age.getMaximumAge()) return;

        // Respect tool (Fortune/Silk) by using player-aware drop path
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, event.getPlayer()));
        if (drops.isEmpty()) return;

        // Prevent vanilla drops; we’ll drop our replacements exactly once
        event.setDropItems(false);

        for (ItemStack is : drops) {
            ItemStack out = is.clone();
            out.setAmount(is.getAmount() * 2); // exactly 2× baseline drops
            block.getWorld().dropItemNaturally(block.getLocation(), out);
        }

        // Mark processed to prevent any second pass (from other listeners/plugins)
        block.setMetadata(META_BLOCK_DBL, new FixedMetadataValue(CCUtilities.getInstance(), Boolean.TRUE));
    }
}
