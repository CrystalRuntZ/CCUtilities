package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.Random;

public class MidasPlightEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Golden Touch";
    private static final Random RNG = new Random();

    // Common items
    private static final Material[] COMMON_ITEMS = {
            Material.RAW_GOLD,
            Material.GOLD_ORE,
            Material.GOLD_INGOT,
            Material.GOLD_NUGGET
    };

    // Rare items (1/10th as likely as a common item)
    private static final Material[] RARE_ITEMS = {
            Material.GOLD_BLOCK,
            Material.RAW_GOLD_BLOCK
    };

    // Weight ratio: common:rare = 10:1
    private static final int COMMON_WEIGHT = 10;
    private static final int RARE_WEIGHT   = 1;

    @Override public String getIdentifier() { return "midas_plight"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType().name().endsWith("_PICKAXE");
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
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!hasEnchant(tool)) return;

        Block block = event.getBlock();
        if (block.getType() != Material.STONE) return;

        // 15% chance to replace normal drop with a weighted gold-themed item
        if (RNG.nextDouble() <= 0.15) {
            event.setDropItems(false);
            Material drop = weightedGoldItem();
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(drop, 1));
        }
    }

    /** Pick a gold item where each rare item is 1/10th as likely as each common item. */
    private Material weightedGoldItem() {
        int commonTotal = COMMON_ITEMS.length * COMMON_WEIGHT;
        int rareTotal   = RARE_ITEMS.length   * RARE_WEIGHT;
        int totalWeight = commonTotal + rareTotal;

        int r = RNG.nextInt(totalWeight); // [0, totalWeight)

        if (r < commonTotal) {
            int idx = r / COMMON_WEIGHT;           // each common bucket has size COMMON_WEIGHT
            return COMMON_ITEMS[idx];
        } else {
            int rr = r - commonTotal;
            int idx = rr / RARE_WEIGHT;            // RARE_WEIGHT is 1, but keep generic
            return RARE_ITEMS[idx];
        }
    }
}
