package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomEfficiencyEnchant implements CustomEnchant {

    private static final String BASE_LORE_RAW  = "&7Random Efficiency";
    private static final String BASE_LORE_SECT = BASE_LORE_RAW.replace('&','§');
    private static final String PREFIX_SECT    = "§7Random Efficiency";

    private static final LegacyComponentSerializer LEGACY_SECTION   = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private static final NamespacedKey KEY = new NamespacedKey(CCUtilities.getInstance(), "random_eff_level");

    @Override public String getIdentifier() { return "random_efficiency"; }
    @Override public String getLoreLine()   { return BASE_LORE_RAW; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        String n = item.getType().name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String s = LEGACY_SECTION.serialize(c);
                if (s.equalsIgnoreCase(BASE_LORE_SECT) || s.startsWith(PREFIX_SECT)) return true;
            }
        }

        List<String> str = LoreUtil.readLegacyLore(meta);
        if (str != null) {
            for (String s : str) {
                if (s == null) continue;
                String normalized = LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s));
                if (normalized.equalsIgnoreCase(BASE_LORE_SECT) || normalized.startsWith(PREFIX_SECT)) return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item) || hasEnchant(item)) return item;
        setLevelAndLore(item, randomLevel());
        return item;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!appliesTo(tool) || !hasEnchant(tool)) return;
        ensureInitialized(tool);
        setLevelAndLore(tool, randomLevel());
        event.getPlayer().getInventory().setItemInMainHand(tool);
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (appliesTo(item) && hasEnchant(item)) ensureInitialized(item);
    }

    @Override
    public void onPlayerMove(org.bukkit.entity.Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (appliesTo(main) && hasEnchant(main)) ensureInitialized(main);
        ItemStack off = player.getInventory().getItemInOffHand();
        if (appliesTo(off) && hasEnchant(off)) ensureInitialized(off);
    }

    // ---------- helpers ----------

    private int randomLevel() {
        return ThreadLocalRandom.current().nextInt(1, 8); // 1..7
    }

    private void ensureInitialized(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !appliesTo(item)) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer cur = pdc.get(KEY, PersistentDataType.INTEGER);
        if (cur != null && cur > 0) { setLevelAndLore(item, cur); return; }

        Integer parsed = parseLevelFromLore(meta); // supports Roman numerals or integers
        int lvl = (parsed != null && parsed >= 1 && parsed <= 7) ? parsed : randomLevel();
        setLevelAndLore(item, lvl);
    }

    private Integer parseLevelFromLore(ItemMeta meta) {
        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String s = LEGACY_SECTION.serialize(c);
                if (s.startsWith(PREFIX_SECT)) {
                    Integer v = parseSuffixLevel(s);
                    if (v != null) return v;
                }
            }
        }
        List<String> str = LoreUtil.readLegacyLore(meta);
        if (str != null) {
            for (String s : str) {
                String normalized = LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s == null ? "" : s));
                if (normalized.startsWith(PREFIX_SECT)) {
                    Integer v = parseSuffixLevel(normalized);
                    if (v != null) return v;
                }
            }
        }
        return null;
    }

    // Parses either "… <int>" or "… <roman>" (I–VII) at the end.
    private Integer parseSuffixLevel(String s) {
        String[] parts = s.trim().split(" ");
        if (parts.length < 3) return null;
        String last = parts[parts.length - 1].trim();
        try { return Integer.parseInt(last); } catch (NumberFormatException ignored) {}
        return fromRoman(last);
    }

    private void setLevelAndLore(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) return;

        // 1) Remove any existing "Random Efficiency ..." line (any level) to avoid duplicates.
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
            List<String> strLore = LoreUtil.readLegacyLore(meta);
            if (strLore != null) {
                for (String s : strLore) {
                    String normalized = LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(s));
                    lore.add(LEGACY_SECTION.deserialize(normalized).decoration(TextDecoration.ITALIC, false));
                }
            }
        } else {
            lore = new ArrayList<>(lore);
        }

        boolean removedAny = false;
        for (int i = 0; i < lore.size(); ) {
            String s = LEGACY_SECTION.serialize(lore.get(i));
            if (s.equalsIgnoreCase(BASE_LORE_SECT) || s.startsWith(PREFIX_SECT)) {
                lore.remove(i);
                removedAny = true;
            } else {
                i++;
            }
        }
        if (removedAny) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        // 2) Insert the new level line at the VERY TOP (non-italic).
        Component line = loreLineForLevel(level);
        LoreUtil.addLoreAtTop(item, line);

        // 3) Update PDC + enchant (re-fetch meta after LoreUtil modified it).
        meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, level);
        meta.addEnchant(Enchantment.EFFICIENCY, level, true); // allow 6–7
        item.setItemMeta(meta);
    }

    private Component loreLineForLevel(int level) {
        String text = PREFIX_SECT + " " + toRoman(level);
        return LEGACY_SECTION.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    // --- Roman numeral helpers (simple I..VII support) ---

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            default -> String.valueOf(n);
        };
    }

    private Integer fromRoman(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase();
        return switch (t) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            default -> null;
        };
    }
}
