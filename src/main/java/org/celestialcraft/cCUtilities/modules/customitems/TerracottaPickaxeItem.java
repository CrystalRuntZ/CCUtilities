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
import java.util.concurrent.ThreadLocalRandom;

public class TerracottaPickaxeItem implements CustomItem {

    private static final String LORE_LINE = "§7Terracotta Pickaxe";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    private static final Material[] COLORS = {
            Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.MAGENTA_TERRACOTTA,
            Material.LIGHT_BLUE_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA,
            Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA,
            Material.CYAN_TERRACOTTA, Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
            Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA, Material.RED_TERRACOTTA,
            Material.BLACK_TERRACOTTA, Material.TERRACOTTA
    };

    @Override
    public String getIdentifier() {
        return "terracotta_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onBlockBreak(Player player, Block block, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;
        if (block.getType() != Material.STONE) return;

        event.setDropItems(false);

        var drop = new ItemStack(COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)], 1);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);

        player.playSound(player.getLocation(), "block.stone.break", 1f, 1f);
    }

}
