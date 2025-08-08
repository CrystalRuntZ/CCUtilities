package org.celestialcraft.cCUtilities.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

public class SpawnerDataUtil {

    private static final NamespacedKey SPAWNER_TYPE_KEY =
            new NamespacedKey(CCUtilities.getInstance(), "spawner_type");

    public static void setSpawnerType(ItemStack item, EntityType type) {
        if (item == null || type == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(SPAWNER_TYPE_KEY, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
    }

    public static EntityType getSpawnerType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String typeName = meta.getPersistentDataContainer().get(SPAWNER_TYPE_KEY, PersistentDataType.STRING);
        try {
            return typeName == null ? null : EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
