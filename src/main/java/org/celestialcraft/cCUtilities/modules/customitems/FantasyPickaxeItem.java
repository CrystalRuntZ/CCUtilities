package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FantasyPickaxeItem implements CustomItem {

    private static final String LORE_LINE = "§7Fantasy Pickaxe";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "fantasy_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE && item.getType() != Material.DIAMOND_PICKAXE) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onBlockBreak(Player player, org.bukkit.block.Block block, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;
        if (block.getType() != Material.STONE) return;

        event.setCancelled(true);
        block.setType(Material.AIR);

        Location dropLoc = block.getLocation();
        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1–100

        if (roll <= 75) {
            if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.STONE, 1));
            } else {
                dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.COBBLESTONE, 1));
            }
            return;
        }

        if (roll <= 85) { // Potions
            Material[] potions = {
                    Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION // Placeholder, specify actual potions if needed
            };
            // You can't store potion effects in ItemStack easily without meta, so spawn basic potions
            dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(potions[ThreadLocalRandom.current().nextInt(potions.length)], 1));
            return;
        }

        if (roll <= 95) { // Foods
            Material[] foods = {
                    Material.APPLE, Material.BREAD, Material.CARROT, Material.COOKED_BEEF,
                    Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.BAKED_POTATO,
                    Material.MELON_SLICE, Material.GOLDEN_APPLE, Material.BEETROOT, Material.COOKED_COD,
                    Material.COOKED_SALMON, Material.COOKED_MUTTON, Material.COOKIE,
                    Material.PUMPKIN_PIE, Material.ROTTEN_FLESH, Material.POISONOUS_POTATO
            };
            dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(foods[ThreadLocalRandom.current().nextInt(foods.length)], 1));
            return;
        }

        // 96–100: Ores
        Material[] ores = {
                Material.COAL, Material.RAW_IRON, Material.RAW_GOLD, Material.LAPIS_LAZULI,
                Material.REDSTONE, Material.RAW_COPPER, Material.QUARTZ, Material.AMETHYST_SHARD
        };
        dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(ores[ThreadLocalRandom.current().nextInt(ores.length)], 1));
    }
}
