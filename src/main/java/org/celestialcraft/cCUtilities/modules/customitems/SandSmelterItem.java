package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SandSmelterItem implements CustomItem {

    private static final String LORE_LINE = "§7Sand Smelter";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "sand_smelter";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.getType().isItem() || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component c : lore) {
            if (LORE_LINE.equals(serializer.serialize(c))) return true;
        }
        return false;
    }

    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        var block = event.getBlock();
        var type = block.getType();
        if (type == Material.SAND || type == Material.RED_SAND) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GLASS, 1));
        }
    }
}
