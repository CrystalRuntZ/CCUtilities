package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SandmansShovelItem implements CustomItem {

    private static final String RAW_LORE = "&7Sandmans Shovel";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    @Override
    public String getIdentifier() {
        return "sandmans_shovel";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SHOVEL && item.getType() != Material.NETHERITE_SHOVEL) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onBlockBreak(Player player, Block block, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;

        Material type = block.getType();
        if (type == Material.SAND || type == Material.RED_SAND) {
            event.setDropItems(false);
            block.setType(Material.AIR);
            ItemStack drop = new ItemStack(type, 2);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }
}
