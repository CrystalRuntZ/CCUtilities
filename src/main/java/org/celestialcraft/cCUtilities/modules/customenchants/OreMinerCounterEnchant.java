package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.ArrayList;
import java.util.List;

public class OreMinerCounterEnchant implements CustomEnchant {

    private static final String BASE_LORE_RAW  = "&7Ore Miner Counter";
    private static final String BASE_LORE_SECT = BASE_LORE_RAW.replace('&','§');
    private static final String COUNTER_MM     = "<gray>Ores Mined:</gray> <#c1adfe>%d</#c1adfe>";
    private static final String COUNTER_PREFIX_LEGACY = "§7Ores Mined: ";

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SECTION   = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private static final NamespacedKey KEY = new NamespacedKey(CCUtilities.getInstance(), "ores_mined");
    private static final String META_BLOCK_ONCE = "ccu_ore_counter_done";

    @Override public String getIdentifier() { return "ore_miner_counter"; }
    @Override public String getLoreLine()   { return BASE_LORE_RAW; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType().name().endsWith("_PICKAXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        List<Component> compLore = meta.lore();
        if (compLore != null) {
            for (Component c : compLore) {
                String s = LEGACY_SECTION.serialize(c);
                if (s.equalsIgnoreCase(BASE_LORE_SECT)) return true;
                if (isCounterLine(s)) return true;
            }
        }

        List<String> strLore = LoreUtil.readLegacyLore(meta);
        if (strLore != null) {
            for (String s : strLore) {
                if (s == null) continue;
                String norm = LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s));
                if (norm.equalsIgnoreCase(BASE_LORE_SECT)) return true;
                if (isCounterLine(norm)) return true;
            }
        }
        return false;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item) || hasEnchant(item)) return item;

        // 1) Pin base tag at the very top.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        // 2) Init PDC count if missing.
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(KEY, PersistentDataType.INTEGER)) {
            pdc.set(KEY, PersistentDataType.INTEGER, 0);
        }
        item.setItemMeta(meta);

        // 3) Ensure exactly one counter line right after the tag block.
        removeAllCounterLines(item);
        LoreUtil.addLoreAfterTagBlock(item, counterLine(0));
        return item;
    }

    private void ensureInitialized(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        if (isEnchantedBook(item)) return;        // do not modify enchanted books
        if (!appliesTo(item)) return;

        // Always ensure the base tag is at the top.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        ItemMeta meta = item.getItemMeta();
        int current = meta.getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.INTEGER, 0);

        // Remove any stale counters; insert one after the tag block with current value.
        removeAllCounterLines(item);
        LoreUtil.addLoreAfterTagBlock(item, counterLine(current));

        meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, current);
        item.setItemMeta(meta);
    }

    private Component counterLine(int count) {
        return MM.deserialize(String.format(COUNTER_MM, count));
    }

    private boolean isOre(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_ORE") || n.equals("ANCIENT_DEBRIS");
    }

    private void addMine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        if (isEnchantedBook(item)) return;        // do not modify enchanted books

        // Ensure base tag is present at top.
        LoreUtil.ensureLoreAtTop(item, BASE_LORE_RAW);

        // Compute next count
        ItemMeta metaPre = item.getItemMeta();
        PersistentDataContainer pdcPre = metaPre.getPersistentDataContainer();
        int next = Math.max(0, pdcPre.getOrDefault(KEY, PersistentDataType.INTEGER, 0) + 1);

        // Try to update an existing counter line in-place (do NOT touch the base tag)
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) {
            List<String> str = LoreUtil.readLegacyLore(meta);
            lore = new ArrayList<>();
            if (str != null) {
                for (String s : str) {
                    String norm = LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s == null ? "" : s));
                    lore.add(LEGACY_SECTION.deserialize(norm));
                }
            }
        }

        boolean updated = false;
        for (int i = 0; i < lore.size(); i++) {
            String s = LEGACY_SECTION.serialize(lore.get(i));
            if (isCounterLine(s)) {
                lore.set(i, counterLine(next));
                meta.lore(lore);
                updated = true;
                break;
            }
        }

        if (!updated) {
            // No counter line found; clean stale variations and insert a fresh one after the tag block.
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
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack pick = event.getPlayer().getInventory().getItemInMainHand();
        if (isEnchantedBook(pick)) return;                // do not modify enchanted books
        if (!appliesTo(pick) || !hasEnchant(pick)) return;

        Block block = event.getBlock();
        if (!isOre(block.getType())) return;

        // Guard: count once per ore block (prevents +2 if event fires twice)
        if (block.hasMetadata(META_BLOCK_ONCE)) return;

        ensureInitialized(pick);
        addMine(pick);

        block.setMetadata(META_BLOCK_ONCE, new FixedMetadataValue(CCUtilities.getInstance(), Boolean.TRUE));
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (isEnchantedBook(item)) return;                // do not modify enchanted books
        if (appliesTo(item) && hasEnchant(item)) ensureInitialized(item);
    }

    public void onPlayerMove(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!isEnchantedBook(main) && appliesTo(main) && hasEnchant(main)) ensureInitialized(main);
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!isEnchantedBook(off) && appliesTo(off) && hasEnchant(off)) ensureInitialized(off);
        for (ItemStack a : player.getInventory().getArmorContents()) {
            if (!isEnchantedBook(a) && appliesTo(a) && hasEnchant(a)) { ensureInitialized(a); break; }
        }
    }

    private boolean isEnchantedBook(ItemStack item) {
        return item != null && item.getType() == Material.ENCHANTED_BOOK;
    }

    private boolean isCounterLine(String serializedSection) {
        if (serializedSection == null) return false;
        return serializedSection.startsWith(COUNTER_PREFIX_LEGACY)
                || stripSection(serializedSection).startsWith("Ores Mined:");
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
