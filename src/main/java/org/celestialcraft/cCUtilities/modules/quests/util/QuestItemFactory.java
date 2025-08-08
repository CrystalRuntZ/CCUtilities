package org.celestialcraft.cCUtilities.modules.quests.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;

public class QuestItemFactory {

    public static ItemStack createQuestItem(Quest quest) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = "Quest: " + capitalize(quest.getType().name().toLowerCase().replace('_', ' '));
        Component displayName = Component.text(name).color(NamedTextColor.GOLD);
        meta.displayName(displayName); // Modern Adventure API setter
        item.setItemMeta(meta);

        LoreUtils.markQuestItem(item, quest.getId().toString());
        LoreUtils.updateLore(item, quest);

        return item;
    }

    private static String capitalize(String input) {
        if (input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
