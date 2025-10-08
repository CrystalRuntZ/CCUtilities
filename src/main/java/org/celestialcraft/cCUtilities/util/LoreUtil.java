package org.celestialcraft.cCUtilities.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor; // NEW
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class LoreUtil {

    private LoreUtil() {}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMP    = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Replace '&' with '§' so "&7Text" becomes the stored "§7Text". */
    public static String colorize(String raw) {
        return raw == null ? "" : raw.replace('&', '§');
    }

    /**
     * Returns true if the item has a lore line equal to the given raw lore line (e.g., "&7My Lore"),
     * comparing Component lore and any legacy string lore, while IGNORING italics.
     */
    public static boolean itemHasLore(ItemStack item, String rawLoreLine) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        final String target = colorize(rawLoreLine);

        // Check Component lore (normalize italics OFF before serializing)
        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                if (c == null) continue;
                String normalized = LEGACY.serialize(c.decoration(TextDecoration.ITALIC, false));
                if (normalized.equalsIgnoreCase(target)) return true;
            }
        }

        // Check legacy String lore (what anvils/books might have written)
        List<String> legacy = readLegacyLore(meta);
        if (legacy != null) {
            for (String s : legacy) {
                if (s == null) continue;
                // direct § match
                if (s.equalsIgnoreCase(target)) return true;
                // best-effort strip of italic code if present
                String noItal = s.replace("§o", "").replace("&o", "");
                if (noItal.equalsIgnoreCase(target)) return true;
            }
        }
        return false;
    }

    /** Ensure the EXACT lore line exists as a non-italic Component. (appends to end) */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean addLoreIfMissingNoItalics(ItemStack item, String rawLoreLine) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String targetPlain = colorize(rawLoreLine);
        Component exact = LEGACY.deserialize(targetPlain).decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        boolean foundIgnoringItalics = false;
        boolean changed = false;
        List<Component> fixed = new ArrayList<>(lore.size());

        for (Component line : lore) {
            if (line == null) continue;
            String normalized = LEGACY.serialize(line.decoration(TextDecoration.ITALIC, false));
            if (normalized.equalsIgnoreCase(targetPlain)) {
                if (!line.equals(exact)) changed = true;
                fixed.add(exact);
                foundIgnoringItalics = true;
            } else {
                fixed.add(line);
            }
        }

        if (!foundIgnoringItalics) {
            fixed.add(exact);
            changed = true;
        }

        if (changed) {
            meta.lore(fixed);
            item.setItemMeta(meta);
        }
        return changed;
    }

    /** Back-compat alias: all code paths now go through the non-italic writer. */
    public static boolean addLoreIfMissing(ItemStack item, String rawLoreLine) {
        return addLoreIfMissingNoItalics(item, rawLoreLine);
    }

    /** Reads legacy String lore safely. Only used for detection/migration. */
    @SuppressWarnings("deprecation")
    public static List<String> readLegacyLore(ItemMeta meta) {
        return (meta != null && meta.hasLore()) ? meta.getLore() : null;
    }

    // ---------------------------
    // Helpers to remove italics everywhere and canonicalize lore
    // ---------------------------

    @SuppressWarnings("UnusedReturnValue")
    public static boolean normalizeAllLoreToNonItalicComponents(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        boolean changed = false;

        // 1) If legacy String lore exists, convert to Component lore with italic=false
        @SuppressWarnings("deprecation")
        List<String> legacy = (meta.hasLore() ? meta.getLore() : null);
        if (legacy != null && !legacy.isEmpty()) {
            List<Component> out = new ArrayList<>(legacy.size());
            for (String s : legacy) {
                if (s == null) continue;
                Component c = LEGACY.deserialize(s).decoration(TextDecoration.ITALIC, false);
                out.add(c);
            }
            meta.lore(out);
            changed = true;
        } else {
            // 2) If we already have Component lore, rewrite each line with italic=false
            List<Component> comp = meta.lore();
            if (comp != null && !comp.isEmpty()) {
                List<Component> out = new ArrayList<>(comp.size());
                boolean any = false;
                for (Component line : comp) {
                    if (line == null) continue;
                    Component fixed = line.decoration(TextDecoration.ITALIC, false);
                    if (!fixed.equals(line)) any = true;
                    out.add(fixed);
                }
                if (any) {
                    meta.lore(out);
                    changed = true;
                }
            }
        }

        if (changed) item.setItemMeta(meta);
        return changed;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean normalizeDisplayNameNonItalic(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Component name = meta.displayName();
        if (name == null) return false;

        Component fixed = name.decoration(TextDecoration.ITALIC, false);
        if (fixed.equals(name)) return false;

        meta.displayName(fixed);
        item.setItemMeta(meta);
        return true;
    }

    /** Convenience: normalize both lore and display name to non-italic Components. */
    public static void normalizeAllTextNonItalic(ItemStack item) {
        normalizeAllLoreToNonItalicComponents(item);
        normalizeDisplayNameNonItalic(item);
    }

    // ---------------------------
    // LEGACY (bullet-proof): write §-lore with §r prefix so clients never italicize
    // ---------------------------

    /** Serialize any item's lore to legacy strings, prefixing each line with §r and stripping §o. */
    @SuppressWarnings("deprecation")
    public static List<String> serializeToLegacyWithReset(ItemStack item) {
        List<String> out = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return out;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return out;

        List<Component> comp = meta.lore();
        if (comp != null && !comp.isEmpty()) {
            for (Component c : comp) {
                String s = LEGACY.serialize(c.decoration(TextDecoration.ITALIC, false)).replace("§o", "");
                if (!s.startsWith("§r")) s = "§r" + s;
                out.add(s);
            }
            return out;
        }

        List<String> legacy = meta.getLore();
        if (legacy != null && !legacy.isEmpty()) {
            for (String s : legacy) {
                if (s == null) continue;
                String norm = LEGACY.serialize(AMP.deserialize(s)).replace("§o", "");
                if (!norm.startsWith("§r")) norm = "§r" + norm;
                out.add(norm);
            }
        }
        return out;
    }

    /** Compare two legacy lines ignoring italics (§o). */
    public static boolean legacyEqualsIgnoringItalics(String a, String b) {
        if (a == null || b == null) return false;
        String A = a.replace("§o", "");
        String B = b.replace("§o", "");
        return A.equalsIgnoreCase(B);
    }

    /**
     * Copy all lore lines from `book` into `target` as legacy strings prefixed with §r.
     * Keeps any existing target lore (converted to legacy+§r), appends book lines not already present.
     * The result is guaranteed non-italic on the client.
     */
    @SuppressWarnings("deprecation")
    public static void copyBookLoreToItemAsLegacyNoItalics(ItemStack book, ItemStack target) {
        if (book == null || target == null || !target.hasItemMeta()) return;

        // Existing target lore (convert to legacy + §r)
        List<String> targetLegacy = serializeToLegacyWithReset(target);

        // Book lore (convert to legacy + §r)
        List<String> bookLegacy = serializeToLegacyWithReset(book);

        // Append book lines that aren't already present (compare ignoring italics)
        for (String line : bookLegacy) {
            boolean found = false;
            for (String have : targetLegacy) {
                if (legacyEqualsIgnoringItalics(have, line)) { found = true; break; }
            }
            if (!found) targetLegacy.add(line);
        }

        // Write legacy lore back
        ItemMeta meta = target.getItemMeta();
        meta.setLore(targetLegacy);
        target.setItemMeta(meta);
    }

    // ---------------------------
    // NEW: Book lookups for mixed MiniMessage / legacy sources
    // ---------------------------

    /** Make a legacy (§) expectation from either MiniMessage or &/§ raw. Italics removed. */
    private static String expectedLegacyNoItalics(String rawOrMM) {
        if (rawOrMM == null) return "";
        if (rawOrMM.contains("<")) {
            Component c = MM.deserialize(rawOrMM).decoration(TextDecoration.ITALIC, false);
            return LEGACY.serialize(c).replace("§o", "");
        }
        return colorize(rawOrMM).replace("§o", "");
    }

    /** Does the enchanted book contain a lore line that matches rawOrMM, ignoring italics? */
    public static boolean bookHasLoreLine(ItemStack book, String rawOrMM) {
        if (book == null || !book.hasItemMeta()) return false;
        ItemMeta meta = book.getItemMeta();
        String expected = expectedLegacyNoItalics(rawOrMM);

        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String s = LEGACY.serialize(c.decoration(TextDecoration.ITALIC, false)).replace("§o", "");
                if (s.equalsIgnoreCase(expected)) return true;
            }
        }
        @SuppressWarnings("deprecation")
        List<String> legacy = meta.getLore();
        if (legacy != null) {
            for (String s : legacy) {
                if (s == null) continue;
                String norm = LEGACY.serialize(AMP.deserialize(s)).replace("§o", "");
                if (norm.equalsIgnoreCase(expected)) return true;
            }
        }
        return false;
    }

    /** Return the exact *Component* line from the book that matches rawOrMM (italics disabled), preserving colors/gradients. */
    public static Component findMatchingBookLineAsComponentNoItalics(ItemStack book, String rawOrMM) {
        if (book == null || !book.hasItemMeta()) return null;
        ItemMeta meta = book.getItemMeta();
        String expected = expectedLegacyNoItalics(rawOrMM);

        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String s = LEGACY.serialize(c.decoration(TextDecoration.ITALIC, false)).replace("§o", "");
                if (s.equalsIgnoreCase(expected)) return c.decoration(TextDecoration.ITALIC, false);
            }
        }
        @SuppressWarnings("deprecation")
        List<String> legacy = meta.getLore();
        if (legacy != null) {
            for (String s : legacy) {
                if (s == null) continue;
                String norm = LEGACY.serialize(AMP.deserialize(s)).replace("§o", "");
                if (norm.equalsIgnoreCase(expected)) {
                    return LEGACY.deserialize(norm).decoration(TextDecoration.ITALIC, false);
                }
            }
        }
        return null;
    }

    // ============================================================
    // NEW: Top-of-lore placement – insert/move lore to the very top
    // ============================================================

    /** Insert a Component lore line at the very top (index 0), non-italic. */
    public static boolean addLoreAtTop(ItemStack item, Component line) {
        if (item == null || line == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore); // copy so we can safely modify

        lore.add(0, line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    /** Insert an &/§ legacy string at the very top (non-italic). */
    public static boolean addLoreAtTop(ItemStack item, String rawLegacy) {
        if (rawLegacy == null) return false;
        Component c = LEGACY.deserialize(colorize(rawLegacy)).decoration(TextDecoration.ITALIC, false);
        return addLoreAtTop(item, c);
    }

    /**
     * Ensure a lore line exists once, and at the correct position (very top).
     * If it exists elsewhere, it is moved. Non-italic enforced.
     */
    public static boolean ensureLoreAtTop(ItemStack item, String rawLegacy) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String target = colorize(rawLegacy);
        Component exact = LEGACY.deserialize(target).decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore); // copy so we can remove while iterating

        // Remove duplicates anywhere (compare ignoring italics)
        for (int i = 0; i < lore.size(); ) {
            Component line = lore.get(i);
            String normalized = LEGACY.serialize(line.decoration(TextDecoration.ITALIC, false));
            if (normalized.equalsIgnoreCase(target)) {
                lore.remove(i);
            } else {
                i++;
            }
        }

        // Insert at top
        lore.add(0, exact);
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    // ============================================================
    // Universal placement – insert/move lore after tag block
    // ============================================================

    /** Insert a Component lore line immediately after the tag/enchant block. */
    public static boolean addLoreAfterTagBlock(ItemStack item, Component line) {
        if (item == null || line == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        int insertAt = computeTagBlockEndIndex(lore);
        lore.add(insertAt, line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    /** Insert an &/§ legacy string after the tag block (non-italic). */
    public static boolean addLoreAfterTagBlock(ItemStack item, String rawLegacy) {
        if (rawLegacy == null) return false;
        Component c = LEGACY.deserialize(colorize(rawLegacy)).decoration(TextDecoration.ITALIC, false);
        return addLoreAfterTagBlock(item, c);
    }

    /**
     * Ensure a lore line exists once, and at the correct position (right after tag block).
     * If it exists elsewhere, it is moved. Non-italic enforced.
     */
    public static boolean ensureLoreAfterTagBlock(ItemStack item, String rawLegacy) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String target = colorize(rawLegacy);
        Component exact = LEGACY.deserialize(target).decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore); // copy so we can remove while iterating

        // Remove duplicates anywhere (compare ignoring italics)
        boolean changed = false;
        for (int i = 0; i < lore.size(); ) {
            Component line = lore.get(i);
            String normalized = LEGACY.serialize(line.decoration(TextDecoration.ITALIC, false));
            if (normalized.equalsIgnoreCase(target)) {
                lore.remove(i);
                changed = true;
            } else {
                i++;
            }
        }

        // Insert at the correct spot
        int insertAt = computeTagBlockEndIndex(lore);
        lore.add(insertAt, exact);
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    // ---------- placement heuristics ----------
    private static int computeTagBlockEndIndex(List<Component> lore) {
        int idx = 0;
        for (int i = 0; i < lore.size(); i++) {
            Component comp = lore.get(i);
            if (comp == null) break;
            String legacy = LEGACY.serialize(comp);
            String stripped = ChatColor.stripColor(legacy).trim();

            if (stripped.isEmpty()) break;       // blank line => stop before it
            if (isDivider(stripped)) break;      // "-----", "====", em-dash, etc.

            if (isSimpleTag(legacy, stripped)) {
                idx = i + 1;                     // keep consuming tag-like lines
            } else {
                break;                           // first non-tag content
            }
        }
        return idx;
    }

    private static boolean isDivider(String s) {
        return s.indexOf('—') >= 0 || s.matches("^[-=]{3,}.*");
    }

    private static boolean isSimpleTag(String legacy, String stripped) {
        if (stripped.isEmpty()) return false;
        if (stripped.length() > 32) return false;   // paragraphs are not tags
        char first = firstColorCode(legacy);
        // treat gray/dark gray/white as the common “tag” colors
        return first == '7' || first == '8' || first == 'f';
    }

    private static char firstColorCode(String legacy) {
        if (legacy == null) return 'r';
        for (int i = 0; i + 1 < legacy.length(); i++) {
            if (legacy.charAt(i) == '§') {
                char c = Character.toLowerCase(legacy.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) >= 0) return c;
            }
        }
        return 'r';
    }
}
