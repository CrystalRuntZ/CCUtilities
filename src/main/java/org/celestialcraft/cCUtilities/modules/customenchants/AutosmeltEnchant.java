package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.metadata.FixedMetadataValue;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;
import org.celestialcraft.cCUtilities.util.MiningPrecedence;

import java.util.*;

public class AutosmeltEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Autosmelt";
    private static final String META_BLOCK_AUTOSMELT = "ccu_autosmelt_done";

    @Override public String getIdentifier() { return "autosmelt"; }
    @Override public String getLoreLine() { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // ✅ pickaxes only
        return item != null && item.getType().name().endsWith("_PICKAXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) { /* no combat effect */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!hasEnchant(tool)) return;

        Block block = event.getBlock();

        // Guard: only run once per block break (prevents double drops if another listener re-enters)
        if (block.hasMetadata(META_BLOCK_AUTOSMELT)) return;

        // Only autosmelt if this tool class is appropriate for the block (sanity + consistency).
        if (!MiningPrecedence.isPreferredTool(block, tool)) return;

        // Use player-aware overload so silk/fortune & loot tables are respected
        List<ItemStack> naturalDrops = new ArrayList<>(block.getDrops(tool, event.getPlayer()));
        if (naturalDrops.isEmpty()) return;

        // Prevent vanilla drops; we’ll drop our replacements exactly once
        event.setDropItems(false);

        // Merge outputs to ensure 1:1 overall (not duplicate stacks of the same smelted item)
        Map<org.bukkit.Material, Integer> totals = new EnumMap<>(org.bukkit.Material.class);

        for (ItemStack drop : naturalDrops) {
            ItemStack smelted = findSmeltResult(event.getPlayer().getServer(), drop);
            if (smelted != null) {
                // Preserve fortune by scaling when furnace result's default amount is 1
                int amount = (smelted.getAmount() == 1) ? drop.getAmount() : smelted.getAmount() * drop.getAmount();
                totals.merge(smelted.getType(), amount, Integer::sum);
            } else {
                // Not smeltable → keep original
                totals.merge(drop.getType(), drop.getAmount(), Integer::sum);
            }
        }

        // Drop merged stacks with respect to max stack sizes
        for (Map.Entry<org.bukkit.Material, Integer> e : totals.entrySet()) {
            org.bukkit.Material mat = e.getKey();
            int remaining = e.getValue();
            int max = mat.getMaxStackSize();
            while (remaining > 0) {
                int take = Math.min(remaining, max);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(mat, take));
                remaining -= take;
            }
        }

        // Mark processed
        block.setMetadata(META_BLOCK_AUTOSMELT, new FixedMetadataValue(CCUtilities.getInstance(), Boolean.TRUE));
    }

    /**
     * Find smelt result for a given INPUT item by scanning registered CookingRecipe<?>.
     * Covers furnace, smoker, and blast furnace without version-specific classes.
     */
    private ItemStack findSmeltResult(Server server, ItemStack input) {
        Iterator<Recipe> it = server.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof CookingRecipe<?> cook) {
                RecipeChoice choice = cook.getInputChoice();
                if (choice.test(input)) {
                    return cook.getResult().clone();
                }
            }
        }
        return null;
    }
}
