package org.celestialcraft.cCUtilities.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public final class ItemChecks {
    private ItemChecks() {}

    public static boolean anyNonAir(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /** Durable = has max durability > 0 (tools, weapons, armor, elytra, shears, flint&steel, etc.). */
    public static boolean isDurable(ItemStack item) {
        return anyNonAir(item) && item.getType().getMaxDurability() > 0;
    }

    public static boolean isPickaxe(ItemStack i) { return hasTypeSuffix(i, "_PICKAXE"); }
    public static boolean isAxe(ItemStack i)     { return hasTypeSuffix(i, "_AXE"); }
    public static boolean isShovel(ItemStack i)  { return hasTypeSuffix(i, "_SHOVEL"); }
    public static boolean isHoe(ItemStack i)     { return hasTypeSuffix(i, "_HOE"); }
    public static boolean isSword(ItemStack i)   { return hasTypeSuffix(i, "_SWORD"); }

    public static boolean isBow(ItemStack i)       { return hasExact(i, Material.BOW); }
    public static boolean isCrossbow(ItemStack i)  { return hasExact(i, Material.CROSSBOW); }
    public static boolean isTrident(ItemStack i)   { return hasExact(i, Material.TRIDENT); }
    public static boolean isMace(ItemStack i)      { return hasExact(i, Material.MACE); } // 1.21+

    public static boolean isWeapon(ItemStack i) {
        return isSword(i) || isAxe(i) || isMace(i) || isTrident(i) || isBow(i) || isCrossbow(i);
    }

    public static boolean isArmor(ItemStack i) {
        if (!anyNonAir(i)) return false;
        String n = i.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    public static boolean hasAnywhere(Player p, Predicate<ItemStack> test) {
        if (p == null || test == null) return false;
        if (test.test(p.getInventory().getItemInMainHand())) return true;
        if (test.test(p.getInventory().getItemInOffHand())) return true;
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            if (test.test(armor)) return true;
        }
        return false;
    }

    // ---- helpers ----
    private static boolean hasTypeSuffix(ItemStack i, String suffix) {
        return anyNonAir(i) && i.getType().name().endsWith(suffix);
    }
    private static boolean hasExact(ItemStack i, Material m) {
        return i != null && i.getType() == m;
    }
}
