package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.*;
import java.util.regex.Pattern;

public class CustomEnchantAnvilStackingListener implements Listener {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();

    // Heuristic: “vanilla-like” enchant line (non-italic, text + optional roman numerals / digits)
    private static final Pattern VANILLA_ENCHANT_LIKE =
            Pattern.compile("^§r(?:§[0-9a-fk-or])*[A-Za-z][A-Za-z 0-9+/\\-()]*?(?: [IVXLCDM]+| [0-9]+)?$");

    // ========================
    // Preview (slot 2)
    // ========================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;

        AnvilInventory inv = event.getInventory();
        ItemStack base = inv.getItem(0);
        ItemStack book = inv.getItem(1);
        if (base == null || book == null) return;
        if (book.getType() != Material.ENCHANTED_BOOK) return;

        ItemStack vanilla = event.getResult();
        ItemStack result = (vanilla == null || vanilla.getType() == Material.AIR) ? base.clone() : vanilla.clone();

        boolean changed = mergeApplicableBookLinesIntoResult(base, book, result);
        if (!changed) return;
        event.setResult(result);
    }

    // ========================
    // Take result (click)
    // ========================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilResultClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AnvilInventory anvil = (AnvilInventory) event.getInventory();
        ItemStack base = anvil.getItem(0);
        ItemStack book = anvil.getItem(1);
        if (base == null || book == null || book.getType() != Material.ENCHANTED_BOOK) return;

        ItemStack out = base.clone();
        boolean added = mergeApplicableBookLinesIntoResult(base, book, out);
        if (!added) return;

        event.setCancelled(true);

        decrementSlot(anvil);        // consume 1 contributing book
        anvil.setItem(0, null);      // consume base (like vanilla)

        if (event.isShiftClick() || !isCursorEmpty(player)) {
            PlayerInventory inv = player.getInventory();
            HashMap<Integer, ItemStack> leftovers = inv.addItem(out);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(st -> player.getWorld().dropItemNaturally(player.getLocation(), st));
            }
        } else {
            player.setItemOnCursor(out);
        }
        player.updateInventory();
    }

    // ========================
    // Core merge logic
    // ========================

    /**
     * Adds CE lines from the book into result:
     * - Only if CE applies to base
     * - Only if the book actually contains that CE line
     * - Skips if result already has the line
     * - Inserts at the VERY TOP of lore (index 0)
     */
    @SuppressWarnings("deprecation")
    private boolean mergeApplicableBookLinesIntoResult(ItemStack base, ItemStack book, ItemStack result) {
        if (base == null || book == null || result == null) return false;

        // Existing result lore (legacy, with §r reset)
        List<String> targetLegacy = LoreUtil.serializeToLegacyWithReset(result);

        // Book lore (legacy, with §r reset)
        List<String> bookLegacy = LoreUtil.serializeToLegacyWithReset(book);

        // “already has” set (ignoring italics)
        List<String> targetNoItal = new ArrayList<>(targetLegacy.size());
        for (String s : targetLegacy) targetNoItal.add(s == null ? "" : s.replace("§o", ""));

        // Precompute all CE display lines (normalized) for detection and duplicates
        Map<CustomEnchant, String> ceExpectedLegacy = new LinkedHashMap<>();
        Map<CustomEnchant, String> ceExpectedNoItal  = new LinkedHashMap<>();
        for (CustomEnchant ce : CustomEnchantRegistry.getAll()) {
            String expected = "§r" + AMP.serialize(AMP.deserialize(LoreUtil.colorize(ce.getLoreLine()))).replace('&', '§');
            String expectedNoItal = expected.replace("§o", "");
            ceExpectedLegacy.put(ce, expected);
            ceExpectedNoItal.put(ce, expectedNoItal);
        }

        // NEW: Insert at TOP of lore instead of after an enchant-like block
        int insertAt = 0;

        boolean changed = false;
        int addedCount = 0;

        // For each CE that applies to base: if the book carries the CE line and result doesn't, INSERT at top (stable order)
        for (CustomEnchant ce : CustomEnchantRegistry.getAll()) {
            if (!ce.appliesTo(base)) continue;

            String expected = ceExpectedLegacy.get(ce);
            String expectedNoItal = ceExpectedNoItal.get(ce);

            // Book contains this CE line?
            boolean bookHas = false;
            for (String line : bookLegacy) {
                if (line == null) continue;
                if (LoreUtil.legacyEqualsIgnoringItalics(line, expected)) { bookHas = true; break; }
            }
            if (!bookHas) continue;

            // Result already has it?
            boolean resultHas = false;
            for (String have : targetNoItal) {
                if (have.equalsIgnoreCase(expectedNoItal)) { resultHas = true; break; }
            }
            if (resultHas) continue;

            // Insert at the TOP (index 0 + addedCount so we keep registry order)
            targetLegacy.add(insertAt + addedCount, expected);
            targetNoItal.add(insertAt + addedCount, expectedNoItal);
            addedCount++;
            changed = true;
        }

        if (!changed) return false;

        // Write back
        ItemMeta meta = result.getItemMeta();
        meta.setLore(targetLegacy);
        result.setItemMeta(meta);
        return true;
    }

    /**
     * (Unused with top insertion) Find the index after the contiguous prefix of “enchant-like” lines.
     * Left here for reference; no longer used since we always insert at index 0.
     */
    @SuppressWarnings("unused")
    private int findEnchantPrefixLength(List<String> lore, Collection<String> knownCeNoItal) {
        if (lore == null || lore.isEmpty()) return 0;

        // Build a fast lookup for known CE “no-ital” lines
        Set<String> known = new HashSet<>();
        for (String s : knownCeNoItal) if (s != null) known.add(s.toLowerCase(Locale.ROOT));

        int i = 0;
        for (; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line == null) break;

            String noItal = line.replace("§o", "");
            boolean matchesKnownCE = known.contains(noItal.toLowerCase(Locale.ROOT));
            boolean matchesVanillaLike = VANILLA_ENCHANT_LIKE.matcher(noItal).matches();

            if (!(matchesKnownCE || matchesVanillaLike)) {
                break; // first non-enchant-like line -> stop
            }
        }
        return i;
    }

    // ========================
    // small helpers
    // ========================

    private static boolean isCursorEmpty(Player p) {
        ItemStack cur = p.getItemOnCursor();
        return cur.getType() == Material.AIR || cur.getAmount() == 0;
    }

    /** Decrement the item amount in the given anvil slot; null it if reaches zero. */
    private static void decrementSlot(AnvilInventory anvil) {
        ItemStack st = anvil.getItem(1);
        if (st == null) return;
        int amt = st.getAmount();
        if (amt <= 1) {
            anvil.setItem(1, null);
        } else {
            st.setAmount(amt - 1);
            anvil.setItem(1, st);
        }
    }
}
