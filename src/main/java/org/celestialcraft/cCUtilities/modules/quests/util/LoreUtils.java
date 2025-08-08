package org.celestialcraft.cCUtilities.modules.quests.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LoreUtils {

    private static final NamespacedKey questKey = new NamespacedKey(CCUtilities.getInstance(), "quest_item");
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static boolean isQuestItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(questKey, PersistentDataType.STRING);
    }

    public static void markQuestItem(ItemStack item, String questId) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(questKey, PersistentDataType.STRING, questId);
        item.setItemMeta(meta);
    }

    public static void updateLore(ItemStack item, Quest quest) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String typeFormatted = capitalizeWords(quest.getType().name().toLowerCase().replace('_', ' '));
        String progressText = "<gray>" + typeFormatted + " <#c1adfe>" + quest.getProgress() + " <gray>/ <#c1adfe>" + quest.getTarget();

        Component formattedLore = mm.deserialize(progressText);
        meta.lore(Collections.singletonList(formattedLore));
        item.setItemMeta(meta);
    }

    public static void sendProgressActionBar(Player player, Quest quest) {
        String typeFormatted = capitalizeWords(quest.getType().name().toLowerCase().replace('_', ' '));
        String progressText = "<gray>" + typeFormatted + " <#c1adfe>" + quest.getProgress() + " <gray>/ <#c1adfe>" + quest.getTarget();
        player.sendActionBar(mm.deserialize(progressText));
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1));
            builder.append(" ");
        }
        return builder.toString().trim();
    }
    public static UUID getQuestId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) return null;

        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) return null;

        for (String line : lore) {
            if (line.contains("Quest ID:")) {
                String idPart = line.substring(line.indexOf("Quest ID:") + 9).trim();
                try {
                    return UUID.fromString(idPart);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
