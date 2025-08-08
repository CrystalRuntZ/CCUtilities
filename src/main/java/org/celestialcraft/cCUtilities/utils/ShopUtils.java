package org.celestialcraft.cCUtilities.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;

import java.util.Arrays;
import java.util.List;

public class ShopUtils {

    public static boolean isShopChest(InventoryHolder holder) {
        if (!(holder instanceof Container container)) return false;
        Sign sign = getAttachedSign(container);
        if (sign == null) return false;

        String text = sign.getLine(0).trim();
        return text.equalsIgnoreCase("[PRICE]");
    }

    public static Sign getAttachedSign(InventoryHolder holder) {
        if (!(holder instanceof Container container)) return null;
        Block baseBlock = container.getBlock();

        List<BlockFace> faces = Arrays.asList(
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP
        );

        for (BlockFace face : faces) {
            Block relative = baseBlock.getRelative(face);
            if (relative.getState() instanceof Sign sign) {
                String text = sign.getLine(0).trim();
                if (text.equalsIgnoreCase("[PRICE]")) {
                    return sign;
                }
            }
        }

        return null;
    }

    public static int countItems(Inventory inv, Material material) {
        int total = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static void removeItems(Inventory inv, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() != material) continue;
            int remove = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - remove);
            remaining -= remove;
            if (item.getAmount() <= 0) inv.remove(item);
            if (remaining <= 0) return;
        }
    }

    public static void defineShop(Player player, String name) {
        ShopDataManager.defineShopRegion(player, name);
        player.sendMessage("<green>Shop region '" + name + "' defined.");
    }
}
