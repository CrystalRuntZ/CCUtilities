package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomItemRegistry {
    private static final List<CustomItem> items = new ArrayList<>();

    public static void register(CustomItem item) {
        items.add(item);
    }

    public static CustomItem get(ItemStack itemStack) {
        if (itemStack == null) return null;
        for (CustomItem item : items) {
            if (item.matches(itemStack)) {
                return item;
            }
        }
        return null;
    }

    public static List<CustomItem> getAll() {
        return Collections.unmodifiableList(items);
    }
}
