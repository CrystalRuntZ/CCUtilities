package org.celestialcraft.cCUtilities.modules.quests.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundle;

import java.util.UUID;

public class WeeklyQuestItemFactory {
    private static final TextColor TITLE_COLOR = TextColor.fromHexString("#c1adfe");

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "weekly_bundle");
    }

    private static String titleFor(WeeklyBundle bundle) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(bundle.getOwner());
        String name = op.getName() != null ? op.getName() : "Player";
        return name + "'s Weekly Quest";
    }

    /** Build the weekly quest paper using LoreUtils for lore formatting (gray label + #c1adfe numbers). */
    public static ItemStack build(Plugin plugin, WeeklyBundle bundle) {
        ItemStack paper = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = paper.getItemMeta();

        Component title = Component.text(titleFor(bundle))
                .color(TITLE_COLOR)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(title);

        // Store bundle id in PDC
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, bundle.getBundleId().toString());
        paper.setItemMeta(meta);

        // Delegate lore formatting to LoreUtils (single source of truth)
        LoreUtils.updateLoreMultiple(paper, bundle.getQuests());
        return paper;
    }

    public static UUID getBundleId(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        try { return s == null ? null : UUID.fromString(s); } catch (Exception e) { return null; }
    }

    /** Keep title + PDC fresh, and delegate lore to LoreUtils to avoid gray/bracket regressions. */
    public static ItemStack syncLore(Plugin plugin, ItemStack item, WeeklyBundle bundle) {
        if (item == null || !item.hasItemMeta()) return item;

        ItemMeta meta = item.getItemMeta();
        Component title = Component.text(titleFor(bundle))
                .color(TITLE_COLOR)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(title);

        // Update PDC bundle id
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, bundle.getBundleId().toString());
        item.setItemMeta(meta);

        // Rewrite quest lore via LoreUtils (gray label + #c1adfe numbers)
        LoreUtils.updateLoreMultiple(item, bundle.getQuests());
        return item;
    }
}
