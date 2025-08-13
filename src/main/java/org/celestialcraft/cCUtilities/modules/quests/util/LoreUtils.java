package org.celestialcraft.cCUtilities.modules.quests.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoreUtils {

    private static final NamespacedKey questKey = new NamespacedKey(CCUtilities.getInstance(), "quest_item");
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

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

        String label = formatQuestLabel(quest);
        String newLine = String.format(
                "<gray>%s:</gray> <#c1adfe>%d</#c1adfe><gray>/</gray><#c1adfe>%d</#c1adfe>",
                label, quest.getProgress(), quest.getTarget()
        );
        Component newComp = mm.deserialize(newLine).decoration(TextDecoration.ITALIC, false);

        List<Component> existing = meta.lore();
        List<Component> out = new ArrayList<>();

        if (existing != null) {
            for (Component c : existing) {
                String txt = normalize(plain.serialize(c));
                if (isProgressLike(txt) || startsWithAnyLabel(txt, List.of(label))) continue;
                out.add(c);
            }
        }

        out.add(newComp);
        meta.lore(out);
        item.setItemMeta(meta);
    }

    public static void updateLoreMultiple(ItemStack item, List<Quest> quests) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> labels = new ArrayList<>();
        for (Quest q : quests) labels.add(formatQuestLabel(q));

        List<Component> existing = meta.lore();
        List<Component> preserved = new ArrayList<>();

        if (existing != null) {
            for (Component c : existing) {
                String txt = normalize(plain.serialize(c));
                if (isProgressLike(txt) || startsWithAnyLabel(txt, labels)) continue;
                preserved.add(c);
            }
        }

        List<Component> out = new ArrayList<>(preserved);
        for (Quest q : quests) {
            String label = formatQuestLabel(q);
            String line = String.format(
                    "<gray>%s:</gray> <#c1adfe>%d</#c1adfe><gray>/</gray><#c1adfe>%d</#c1adfe>",
                    label, q.getProgress(), q.getTarget()
            );
            out.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(out);
        item.setItemMeta(meta);
    }

    public static void sendProgressActionBar(Player player, Quest quest) {
        String label = formatQuestLabel(quest);
        String line = String.format(
                "<gray>%s:</gray> <#c1adfe>%d</#c1adfe><gray>/</gray><#c1adfe>%d</#c1adfe>",
                label, quest.getProgress(), quest.getTarget()
        );
        player.sendActionBar(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
    }

    private static String formatQuestLabel(Quest q) {
        String target = q.getTargetItem();
        String targetPretty = (target == null || target.isBlank())
                ? null
                : capitalizeWords(target.toLowerCase().replace('_', ' '));
        QuestType type = q.getType();
        return switch (type) {
            case KILL_MOBS -> targetPretty != null ? "Kill " + pluralize(targetPretty) : "Kill Mobs";
            case MINE_BLOCK -> targetPretty != null ? "Mine " + pluralize(targetPretty) : "Mine Blocks";
            case PLACE_BLOCKS -> targetPretty != null ? "Place " + pluralize(targetPretty) : "Place Blocks";
            case SMELT_ITEMS -> targetPretty != null ? "Smelt " + pluralize(targetPretty) : "Smelt Items";
            case HARVEST_CROPS -> targetPretty != null ? "Harvest " + targetPretty : "Harvest Crops";
            case BREED_ANIMALS -> targetPretty != null ? "Breed " + pluralize(targetPretty) : "Breed Animals";
            case DISCOVER_BIOME -> targetPretty != null ? "Discover " + targetPretty : "Discover Biome";
            case RUN_DISTANCE -> "Run Distance";
            case SWIM_DISTANCE -> "Swim Distance";
            case ELYTRA_GLIDE -> "Elytra Glide";
            case DAMAGE_MOBS -> "Deal Damage";
            case GAIN_EXPERIENCE -> "Gain Experience";
        };
    }

    private static String pluralize(String s) {
        if (s == null || s.isEmpty()) return s;
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        if (last == 's') return s;
        return s + "s";
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
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) return null;
        for (Component c : lore) {
            String line = plain.serialize(c);
            if (line.contains("Quest ID:")) {
                String idPart = line.substring(line.indexOf("Quest ID:") + 9).trim();
                try { return UUID.fromString(idPart); }
                catch (IllegalArgumentException e) { return null; }
            }
        }
        return null;
    }

    private static boolean isProgressLike(String plainTextLine) {
        if (plainTextLine == null) return false;
        String line = normalize(plainTextLine);
        if (line.matches("(?i)^[^:\\[]+:\\s*\\d+\\s*/\\s*\\d+\\s*$")) return true;
        return line.matches("(?i)^[^:\\[]+\\s*\\[\\s*\\d+\\s*/\\s*\\d+\\s*]\\s*$");
    }

    private static boolean startsWithAnyLabel(String plainTextLine, List<String> labels) {
        if (plainTextLine == null) return false;
        String line = normalize(plainTextLine).toLowerCase();
        for (String lbl : labels) {
            String l = normalize(lbl).toLowerCase();
            if (line.startsWith(l)) return true;
        }
        return false;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String out = s.trim();
        out = out.replace('\u00A0', ' ');
        out = out.replaceAll("^[\\-–—•*>\\s]+", "");
        out = out.replaceAll("\\s+", " ");
        return out;
    }
}
