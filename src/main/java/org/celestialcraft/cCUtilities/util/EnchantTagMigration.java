package org.celestialcraft.cCUtilities.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;

public final class EnchantTagMigration {

    public static boolean migrateTags(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean migrated = false;
        for (String enchantId : CustomEnchantRegistry.getAllIdentifiers()) {
            NamespacedKey oldKey = new NamespacedKey(CCUtilities.getInstance(), "ce:" + enchantId);
            NamespacedKey newKey = new NamespacedKey(CCUtilities.getInstance(), enchantId);
            Byte val = pdc.get(oldKey, PersistentDataType.BYTE);
            if (pdc.has(oldKey, PersistentDataType.BYTE) && !pdc.has(newKey, PersistentDataType.BYTE) && val != null) {
                pdc.set(newKey, PersistentDataType.BYTE, val);
                pdc.remove(oldKey);
                migrated = true;
            }
        }
        if (migrated) item.setItemMeta(meta);
        return migrated;
    }
}
