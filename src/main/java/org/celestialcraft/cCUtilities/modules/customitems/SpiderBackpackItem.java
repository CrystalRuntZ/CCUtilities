package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

import org.celestialcraft.cCUtilities.CCUtilities;

public class SpiderBackpackItem implements CustomItem, Listener {

    private static final String RAW_LORE = "§7Spider Backpack";
    private static final String LORE_SECT = RAW_LORE.replace('&','§');
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final String KEY_ID            = "spider_backpack_id";
    private static final String KEY_DATA          = "spider_backpack_data";
    private static final String KEY_LAST_OPEN     = "spider_backpack_last_open";
    private static final String KEY_LAST_OPEN_TS  = "spider_backpack_last_open_ts";

    private static final String KEY_DATA_BAK1     = "spider_backpack_data_bak1";
    private static final String KEY_DATA_BAK2     = "spider_backpack_data_bak2";
    private static final String KEY_DATA_BAK1_TS  = "spider_backpack_data_bak1_ts";
    private static final String KEY_DATA_BAK2_TS  = "spider_backpack_data_bak2_ts";

    private final JavaPlugin plugin;
    private final Map<UUID, OpenSession> openByPlayer = new HashMap<>();

    public SpiderBackpackItem(JavaPlugin plugin) {
        this.plugin = (plugin == null) ? CCUtilities.getInstance() : plugin;
    }

    public String getIdentifier() { return "spider_backpack"; }
    public String getLoreLine()   { return RAW_LORE; }
    public boolean appliesTo(ItemStack item) { return item != null; }
    public boolean matches(ItemStack item)   { return appliesTo(item) && hasLoreLine(item); }

    private static boolean hasLoreLine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        for (var c : lore) {
            if (c == null) continue;
            if (LEGACY.serialize(c).equalsIgnoreCase(LORE_SECT)) return true;
        }
        return false;
    }

    private NamespacedKey key(String path) { return new NamespacedKey(plugin, path); }

    public String ensureBackpackId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(key(KEY_ID), PersistentDataType.STRING);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            pdc.set(key(KEY_ID), PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return id;
    }

    public String getBackpackId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key(KEY_ID), PersistentDataType.STRING);
    }

    public ItemStack[] loadContents(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ItemStack[27];
        ItemMeta meta = item.getItemMeta();
        String yaml = meta.getPersistentDataContainer().get(key(KEY_DATA), PersistentDataType.STRING);
        if (yaml == null || yaml.isEmpty()) return new ItemStack[27];

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (InvalidConfigurationException ex) {
            plugin.getLogger().warning("[SpiderBackpack] Bad YAML data, resetting: " + ex.getMessage());
            return new ItemStack[27];
        }

        ItemStack[] out = new ItemStack[27];
        List<?> list = cfg.getList("c");
        if (list == null) return out;

        for (int i = 0; i < Math.min(27, list.size()); i++) {
            Object o = list.get(i);
            if (o instanceof Map<?, ?> m) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) m;
                try { out[i] = ItemStack.deserialize(map); }
                catch (Throwable t) { out[i] = null; }
            } else {
                out[i] = null;
            }
        }
        return out;
    }

    public ItemStack[] getBackupContents(ItemStack item, int which) {
        if (item == null || !item.hasItemMeta()) return new ItemStack[0];
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String yaml = switch (which) {
            case 0 -> {
                String bak1 = pdc.get(key(KEY_DATA_BAK1), PersistentDataType.STRING);
                if (bak1 != null && !bak1.isEmpty()) yield bak1;
                String bak2 = pdc.get(key(KEY_DATA_BAK2), PersistentDataType.STRING);
                if (bak2 != null && !bak2.isEmpty()) yield bak2;
                yield null;
            }
            case 1 -> pdc.get(key(KEY_DATA_BAK1), PersistentDataType.STRING);
            case 2 -> pdc.get(key(KEY_DATA_BAK2), PersistentDataType.STRING);
            default -> null;
        };

        if (yaml == null || yaml.isEmpty()) return new ItemStack[0];

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (InvalidConfigurationException ex) {
            plugin.getLogger().warning("[SpiderBackpack] Bad YAML backup data: " + ex.getMessage());
            return new ItemStack[0];
        }

        ItemStack[] out = new ItemStack[27];
        List<?> list = cfg.getList("c");
        if (list == null) return out;

        for (int i = 0; i < Math.min(27, list.size()); i++) {
            Object o = list.get(i);
            if (o instanceof Map<?, ?> m) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) m;
                try { out[i] = ItemStack.deserialize(map); }
                catch (Throwable t) { out[i] = null; }
            } else {
                out[i] = null;
            }
        }
        return out;
    }

    public String restoreLatestBackupWithUndo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String bak1 = pdc.get(key(KEY_DATA_BAK1), PersistentDataType.STRING);
        String bak2 = pdc.get(key(KEY_DATA_BAK2), PersistentDataType.STRING);

        String chosen = (bak1 != null && !bak1.isEmpty()) ? bak1 :
                (bak2 != null && !bak2.isEmpty()) ? bak2 : null;
        if (chosen == null) return null;

        String previous = pdc.get(key(KEY_DATA), PersistentDataType.STRING);
        pdc.set(key(KEY_DATA), PersistentDataType.STRING, chosen);
        item.setItemMeta(meta);
        return previous;
    }

    public String restoreBackupWithUndo(ItemStack item, int which) {
        if (item == null || !item.hasItemMeta()) return null;
        if (which != 1 && which != 2) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String src = pdc.get(key(which == 1 ? KEY_DATA_BAK1 : KEY_DATA_BAK2), PersistentDataType.STRING);
        if (src == null || src.isEmpty()) return null;
        String previous = pdc.get(key(KEY_DATA), PersistentDataType.STRING);
        pdc.set(key(KEY_DATA), PersistentDataType.STRING, src);
        item.setItemMeta(meta);
        return previous;
    }

    public long getBackupTimestamp(ItemStack item, int which) {
        if (item == null || !item.hasItemMeta()) return 0L;
        Long ts = (which == 1)
                ? item.getItemMeta().getPersistentDataContainer().get(key(KEY_DATA_BAK1_TS), PersistentDataType.LONG)
                : item.getItemMeta().getPersistentDataContainer().get(key(KEY_DATA_BAK2_TS), PersistentDataType.LONG);
        return ts == null ? 0L : ts;
    }

    public void saveContentsToItem(ItemStack item, ItemStack[] contents, Player owner) {
        if (item == null || !item.hasItemMeta()) return;

        ItemStack[] sanitized = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            ItemStack s = (contents != null && i < contents.length) ? contents[i] : null;
            if (s == null) { sanitized[i] = null; continue; }

            boolean isBackpack = hasLoreLine(s) || (s.hasItemMeta() && s.getItemMeta().getPersistentDataContainer().has(key(KEY_ID), PersistentDataType.STRING));
            if (isBackpack) {
                if (owner != null) {
                    HashMap<Integer, ItemStack> leftover = owner.getInventory().addItem(s);
                    if (!leftover.isEmpty()) {
                        owner.getWorld().dropItemNaturally(owner.getLocation(), s);
                    }
                }
                sanitized[i] = null;
            } else {
                sanitized[i] = s;
            }
        }

        List<Map<String, Object>> list = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) {
            ItemStack stack = sanitized[i];
            list.add(stack == null ? null : stack.serialize());
        }
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("c", list);
        String newYaml = cfg.saveToString();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentYaml = pdc.get(key(KEY_DATA), PersistentDataType.STRING);

        if (Objects.equals(currentYaml, newYaml)) {
            return;
        }

        rotateBackups(pdc, currentYaml);

        pdc.set(key(KEY_DATA), PersistentDataType.STRING, newYaml);
        item.setItemMeta(meta);
    }

    private void rotateBackups(PersistentDataContainer pdc, String currentYaml) {
        if (currentYaml == null || currentYaml.isEmpty()) return;

        String oldBak1 = pdc.get(key(KEY_DATA_BAK1), PersistentDataType.STRING);
        Long oldBak1Ts = pdc.get(key(KEY_DATA_BAK1_TS), PersistentDataType.LONG);
        if (oldBak1 != null && !oldBak1.isEmpty()) {
            pdc.set(key(KEY_DATA_BAK2), PersistentDataType.STRING, oldBak1);
            pdc.set(key(KEY_DATA_BAK2_TS), PersistentDataType.LONG, oldBak1Ts == null ? System.currentTimeMillis() : oldBak1Ts);
        }

        pdc.set(key(KEY_DATA_BAK1), PersistentDataType.STRING, currentYaml);
        pdc.set(key(KEY_DATA_BAK1_TS), PersistentDataType.LONG, System.currentTimeMillis());
    }

    private void markLastOpener(ItemStack item, UUID opener) {
        if (item == null || !item.hasItemMeta() || opener == null) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key(KEY_LAST_OPEN), PersistentDataType.STRING, opener.toString());
        pdc.set(key(KEY_LAST_OPEN_TS), PersistentDataType.LONG, System.currentTimeMillis());
        item.setItemMeta(meta);
    }

    public UUID getLastOpener(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(key(KEY_LAST_OPEN), PersistentDataType.STRING);
        try { return (s == null) ? null : UUID.fromString(s); }
        catch (IllegalArgumentException ex) { return null; }
    }

    public long getLastOpenTimestamp(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0L;
        Long ts = item.getItemMeta().getPersistentDataContainer().get(key(KEY_LAST_OPEN_TS), PersistentDataType.LONG);
        return ts == null ? 0L : ts;
    }

    public void handleUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (!hasLoreLine(inHand)) return;

        String id = ensureBackpackId(inHand);
        if (id == null) return;

        markLastOpener(inHand, p.getUniqueId());

        Inventory inv = Bukkit.createInventory(p, 27, Component.text("Spider Backpack"));
        inv.setContents(loadContents(inHand));

        openByPlayer.put(p.getUniqueId(), new OpenSession(id, inv));
        p.openInventory(inv);
    }

    public void handleInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        OpenSession sess = openByPlayer.get(p.getUniqueId());
        if (sess == null || e.getInventory() != sess.inventory) return;

        ItemStack target = findItemById(p, sess.backpackId);
        if (target == null) {
            target = p.getInventory().getItemInMainHand();
            if (!matches(target)) target = null;
        }
        if (target != null) {
            saveContentsToItem(target, e.getInventory().getContents(), p);
        } else {
            plugin.getLogger().warning("[SpiderBackpack] Could not locate the backpack item to save contents. Data not saved.");
        }

        openByPlayer.remove(p.getUniqueId());
    }

    public void handleQuit(PlayerQuitEvent e) {
        openByPlayer.remove(e.getPlayer().getUniqueId());
    }

    private ItemStack findItemById(Player p, String id) {
        if (id == null || id.isEmpty()) return null;
        PlayerInventory inv = p.getInventory();
        for (ItemStack it : inv.getContents()) if (itemHasId(it, id)) return it;
        for (ItemStack it : inv.getArmorContents()) if (itemHasId(it, id)) return it;
        if (itemHasId(inv.getItemInOffHand(), id)) return inv.getItemInOffHand();
        return null;
    }

    private boolean itemHasId(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        String v = item.getItemMeta().getPersistentDataContainer().get(key(KEY_ID), PersistentDataType.STRING);
        return id.equals(v);
    }

    private static class OpenSession {
        final String backpackId;
        final Inventory inventory;
        OpenSession(String id, Inventory inv) { this.backpackId = id; this.inventory = inv; }
    }
}
