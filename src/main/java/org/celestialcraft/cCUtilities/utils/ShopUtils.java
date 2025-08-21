package org.celestialcraft.cCUtilities.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ShopUtils {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /**
     * Returns true if the top inventory belongs to a container that has a [PRICE] sign attached.
     */
    public static boolean isShopChest(InventoryHolder holder) {
        if (!(holder instanceof Container)) return false;
        Sign sign = getAttachedSign(holder);
        return sign != null && isPriceTag(sign);
    }

    /**
     * Gives the player the selection wand used by the shops module.
     */
    public static void giveSelectionWand(Player player) {
        ItemStack wand = new ItemStack(Material.STONE_HOE);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(Component.text("Shop Wand", NamedTextColor.AQUA));
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
    }

    /**
     * Finds an attached sign on the container's block (checks the common faces).
     * Only returns a sign that has a [PRICE] tag on either face.
     */
    public static Sign getAttachedSign(InventoryHolder holder) {
        if (!(holder instanceof Container container)) return null;
        Block baseBlock = container.getBlock();

        List<BlockFace> faces = Arrays.asList(
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        );

        for (BlockFace face : faces) {
            Block relative = baseBlock.getRelative(face);
            if (relative.getState() instanceof Sign sign) {
                if (isPriceTag(sign)) {
                    return sign;
                }
            }
        }
        return null;
    }

    /** True if the sign's first line (front or back) is "[PRICE]" in plain text. */
    private static boolean isPriceTag(Sign sign) {
        Component front0 = sign.getSide(Side.FRONT).line(0);
        Component back0  = sign.getSide(Side.BACK).line(0);
        String f = PLAIN.serialize(front0).trim();
        String b = PLAIN.serialize(back0 ).trim();
        return "[PRICE]".equalsIgnoreCase(f) || "[PRICE]".equalsIgnoreCase(b);
    }

    public static int countItems(Inventory inv, Material material) {
        int total = 0;
        if (inv == null || material == null) return 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static void removeItems(Inventory inv, Material material, int amount) {
        if (inv == null || material == null || amount <= 0) return;
        int remaining = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != material) continue;
            int remove = Math.min(item.getAmount(), remaining);
            if (remove <= 0) continue;
            item.setAmount(item.getAmount() - remove);
            remaining -= remove;
            if (item.getAmount() <= 0) inv.clear(i);
            if (remaining == 0) return;
        }
    }

    /**
     * Can this inventory fit the given stack (respecting stacking + empty slots)?
     */
    public static boolean canFit(Inventory inv, ItemStack stack) {
        if (inv == null || stack == null || stack.getType().isAir()) return true;
        int amount = stack.getAmount();
        int maxStack = stack.getMaxStackSize();

        // Stack onto similar items
        for (ItemStack it : inv.getContents()) {
            if (it == null) continue;
            if (it.isSimilar(stack)) {
                int space = maxStack - it.getAmount();
                if (space > 0) {
                    amount -= space;
                    if (amount <= 0) return true;
                }
            }
        }
        // Empty slots
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType().isAir()) {
                amount -= maxStack;
                if (amount <= 0) return true;
            }
        }
        return false;
    }

    /**
     * Normalizes common currency aliases from the sign to actual Materials.
     */
    public static Material parseCurrency(String raw) {
        if (raw == null) return null;
        String key = raw.replaceAll("[^A-Za-z]", "").toUpperCase(); // "Diamond_Block" -> "DIAMONDBLOCK"
        return switch (key) {
            case "DIAMOND" -> Material.DIAMOND;
            case "DIAMONDBLOCK" -> Material.DIAMOND_BLOCK;
            case "EMERALD" -> Material.EMERALD;
            case "EMERALDBLOCK" -> Material.EMERALD_BLOCK;
            case "IRON", "IRONINGOT" -> Material.IRON_INGOT;
            case "IRONBLOCK" -> Material.IRON_BLOCK;
            case "GOLD", "GOLDINGOT" -> Material.GOLD_INGOT;
            case "GOLDBLOCK" -> Material.GOLD_BLOCK;
            case "NETHERITE", "NETHERITEINGOT" -> Material.NETHERITE_INGOT;
            case "NETHERITEBLOCK" -> Material.NETHERITE_BLOCK;
            case "AMETHYST" -> Material.AMETHYST_SHARD;
            case "COPPER", "COPPERINGOT" -> Material.COPPER_INGOT;
            case "COPPERBLOCK" -> Material.COPPER_BLOCK;
            case "COAL" -> Material.COAL;
            case "COALBLOCK" -> Material.COAL_BLOCK;
            case "REDSTONE" -> Material.REDSTONE;
            case "REDSTONEBLOCK" -> Material.REDSTONE_BLOCK;
            case "LAPIS", "LAPISLAZULI" -> Material.LAPIS_LAZULI;
            case "LAPISBLOCK" -> Material.LAPIS_BLOCK;
            default -> Material.matchMaterial(raw.toUpperCase());
        };
    }

    /**
     * Given a sign block, return the container it is attached to (wall signs -> behind; standing -> below).
     */
    public static InventoryHolder findAttachedContainer(Block signBlock) {
        if (signBlock == null) return null;

        Block attached;
        var data = signBlock.getBlockData();
        try {
            if (data instanceof org.bukkit.block.data.type.WallSign wall) {
                attached = signBlock.getRelative(wall.getFacing().getOppositeFace());
            } else {
                // Standing sign: assume attached to the block below
                attached = signBlock.getRelative(BlockFace.DOWN);
            }
        } catch (NoClassDefFoundError err) {
            // Fallback if WallSign class isn't present for some reason
            attached = signBlock.getRelative(BlockFace.DOWN);
        }

        var state = attached.getState();
        return (state instanceof InventoryHolder ih) ? ih : null;
    }
}
