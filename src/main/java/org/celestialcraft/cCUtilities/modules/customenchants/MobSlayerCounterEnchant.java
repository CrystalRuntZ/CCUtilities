package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.ArrayList;
import java.util.List;

public class MobSlayerCounterEnchant implements CustomEnchant {

    private static final String BASE_LORE_RAW  = "&7Mob Slayer Counter";
    private static final String BASE_LORE_SECT = BASE_LORE_RAW.replace('&','§');
    private static final String COUNTER_MM     = "<gray>Mobs Slayed:</gray> <#c1adfe>%d</#c1adfe>";
    private static final String COUNTER_PREFIX_LEGACY = "§7Mobs Slayed: ";

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SECTION   = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private static final NamespacedKey KEY = new NamespacedKey(CCUtilities.getInstance(), "mob_slays");

    @Override public String getIdentifier() { return "mob_slayer_counter"; }
    @Override public String getLoreLine()   { return BASE_LORE_RAW; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String s = LEGACY_SECTION.serialize(c);
                if (equalsBaseTag(s) || isCounterLine(s)) return true;
            }
        }

        List<String> lines = LoreUtil.readLegacyLore(meta);
        if (lines != null) {
            for (String s : lines) {
                if (equalsBaseTagNormalized(s) || isCounterLine(stripSection(normalizeToSection(s)))) return true;
            }
        }
        return false;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item) || hasEnchant(item)) return item;

        // 1) Pin the base tag at the VERY TOP of lore.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        // 2) Initialize PDC (count = 0) if absent.
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(KEY, PersistentDataType.INTEGER)) {
            pdc.set(KEY, PersistentDataType.INTEGER, 0);
        }
        item.setItemMeta(meta);

        // 3) Ensure exactly ONE counter line, positioned right after the tag block.
        removeAllCounterLines(item);
        LoreUtil.addLoreAfterTagBlock(item, counterLine(0));

        return item;
    }

    private void ensureInitialized(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        if (isEnchantedBook(item)) return;     // ← do not modify books
        if (!appliesTo(item)) return;

        // Always pin the base tag at top.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        // Read current count (default 0)
        ItemMeta meta = item.getItemMeta();
        int current = meta.getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.INTEGER, 0);

        // Remove any stale counter lines, then insert one after the tag block.
        removeAllCounterLines(item);
        LoreUtil.addLoreAfterTagBlock(item, counterLine(current));

        // Persist current value explicitly (ensures the key exists)
        meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, current);
        item.setItemMeta(meta);
    }

    private Component counterLine(int count) {
        return MM.deserialize(String.format(COUNTER_MM, count));
    }

    private void addKill(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        if (isEnchantedBook(item)) return;     // ← do not modify books

        // Ensure base tag is present at top.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        // Compute next count
        ItemMeta metaPre = item.getItemMeta();
        PersistentDataContainer pdcPre = metaPre.getPersistentDataContainer();
        int next = Math.max(0, pdcPre.getOrDefault(KEY, PersistentDataType.INTEGER, 0) + 1);

        // Try to update existing counter line in place
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) {
            List<String> str = LoreUtil.readLegacyLore(meta);
            lore = new ArrayList<>();
            if (str != null) {
                for (String s : str) lore.add(LEGACY_SECTION.deserialize(normalizeToSection(s)));
            }
        }

        boolean updated = false;
        for (int i = 0; i < lore.size(); i++) {
            String s = LEGACY_SECTION.serialize(lore.get(i));
            if (isCounterLine(s)) { // ← only update the counter line (do NOT replace the base tag)
                lore.set(i, counterLine(next));
                meta.lore(lore);
                updated = true;
                break;
            }
        }

        if (!updated) {
            // No counter line found; remove any stale variants, then insert a fresh one after the tag block.
            meta.lore(removeCounterFrom(lore));
            item.setItemMeta(meta);
            LoreUtil.addLoreAfterTagBlock(item, counterLine(next));
            meta = item.getItemMeta(); // re-fetch after insertion
        }

        // Write PDC and commit
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, next);
        item.setItemMeta(meta);
    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (isEnchantedBook(weapon)) return;           // ← do not modify books
        if (!appliesTo(weapon) || !hasEnchant(weapon)) return;

        ensureInitialized(weapon);
        addKill(weapon);
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (isEnchantedBook(item)) return;             // ← do not modify books
        if (appliesTo(item) && hasEnchant(item)) ensureInitialized(item);
    }

    // ----- helpers -----

    private boolean isEnchantedBook(ItemStack item) {
        return item != null && item.getType() == Material.ENCHANTED_BOOK;
    }

    private boolean equalsBaseTag(String s) { return s != null && s.equalsIgnoreCase(BASE_LORE_SECT); }
    private boolean equalsBaseTagNormalized(String s) { return s != null && normalizeToSection(s).equalsIgnoreCase(BASE_LORE_SECT); }

    private boolean isCounterLine(String serializedSection) {
        if (serializedSection == null) return false;
        return serializedSection.startsWith(COUNTER_PREFIX_LEGACY)
                || stripSection(serializedSection).startsWith("Mobs Slayed:");
    }

    private String normalizeToSection(String s) {
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s == null ? "" : s));
    }

    private String stripSection(String s) {
        if (s == null) return "";
        return s.replaceAll("§x(§[0-9a-fA-F]){6}", "")
                .replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    /** Remove every existing counter line (any number) from the item's lore. */
    private void removeAllCounterLines(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;

        List<Component> cleaned = removeCounterFrom(lore);
        if (cleaned != lore) {
            meta.lore(cleaned);
            item.setItemMeta(meta);
        }
    }

    /** Returns a copy of `lore` without any counter lines. */
    private List<Component> removeCounterFrom(List<Component> lore) {
        List<Component> out = new ArrayList<>(lore.size());
        for (Component c : lore) {
            if (c == null) continue;
            String s = LEGACY_SECTION.serialize(c);
            if (!isCounterLine(s)) out.add(c);
        }
        return out;
    }
}
