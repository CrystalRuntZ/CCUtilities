package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;

public class CyberStorageItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Cyber Storage";
    private static final Component GUI_TITLE = Component.text("Cyber Storage");
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    private final Map<String, Inventory> storageInventories = new HashMap<>();
    private final File dataFile;
    private final FileConfiguration dataConfig;
    private final NamespacedKey storageKey;
    private final JavaPlugin plugin;

    public CyberStorageItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageKey = new NamespacedKey(plugin, "cyberstorage_id");
        this.dataFile = new File(plugin.getDataFolder(), "cyberstorage-data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadInventoryData();
    }

    @Override
    public String getIdentifier() {
        return "cyber_storage";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(storageKey, PersistentDataType.STRING)) return true;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER.replace("&", "§"))) return true;
        }

        return false;
    }

    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getHand() == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!matches(item)) return;
        event.setCancelled(true);

        String id = getOrCreateStorageId(item);
        if (id == null) {
            player.sendMessage("§cFailed to open Cyber Storage: no ID found.");
            return;
        }

        Inventory inv = storageInventories.computeIfAbsent(id, key ->
                Bukkit.createInventory(null, 27, GUI_TITLE));

        player.openInventory(inv);
        player.sendMessage("§eCyber Storage opened.");
    }

    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && matches(clicked)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§cYou cannot store this item inside its own Cyber Storage.");
        }
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;

        Player player = (Player) event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        String id = getOrCreateStorageId(held);
        if (id == null) return;

        Inventory inv = event.getInventory();
        storageInventories.put(id, inv);
        saveInventoryData(id, inv);
    }

    private String getOrCreateStorageId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(storageKey, PersistentDataType.STRING)) {
            return container.get(storageKey, PersistentDataType.STRING);
        }

        String newId = UUID.randomUUID().toString();
        container.set(storageKey, PersistentDataType.STRING, newId);
        item.setItemMeta(meta);
        return newId;
    }

    @SuppressWarnings("deprecation")
    private void saveInventoryData(String id, Inventory inventory) {
        List<String> base64Items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                     BukkitObjectOutputStream out = new BukkitObjectOutputStream(byteOut)) {
                    out.writeObject(item);
                    base64Items.add(Base64.getEncoder().encodeToString(byteOut.toByteArray()));
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to serialize CyberStorage item: " + e.getMessage());
                    base64Items.add(null);
                }
            } else {
                base64Items.add(null);
            }
        }

        dataConfig.set("inventories." + id, base64Items);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save CyberStorage data: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void loadInventoryData() {
        if (!dataConfig.contains("inventories")) return;

        ConfigurationSection section = dataConfig.getConfigurationSection("inventories");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            List<?> stored = dataConfig.getList("inventories." + id);
            Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);

            if (stored != null) {
                for (int i = 0; i < stored.size(); i++) {
                    Object value = stored.get(i);
                    if (value == null) continue;
                    try {
                        ItemStack item;
                        if (value instanceof String base64) {
                            byte[] bytes = Base64.getDecoder().decode(base64);
                            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                                item = (ItemStack) in.readObject();
                            }
                        } else if (value instanceof Map) {
                            item = ItemStack.deserialize((Map<String, Object>) value);
                        } else continue;
                        inventory.setItem(i, item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load CyberStorage item: " + e.getMessage());
                    }
                }
            }

            storageInventories.put(id, inventory);
        }
    }
}
