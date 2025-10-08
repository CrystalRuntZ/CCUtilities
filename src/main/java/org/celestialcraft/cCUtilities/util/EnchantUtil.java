package org.celestialcraft.cCUtilities.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

public final class EnchantUtil {
    private EnchantUtil(){}

    private static NamespacedKey keyFor(String id) {
        return new NamespacedKey(CCUtilities.getInstance(), id);
    }

    public static boolean hasTag(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyFor(id), PersistentDataType.BYTE);
    }

    public static void setTag(ItemStack item, String id) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keyFor(id), PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
    }
}
