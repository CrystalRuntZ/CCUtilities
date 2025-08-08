package org.celestialcraft.cCUtilities.modules.activity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CelestialShopManager {

    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public CelestialShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void openShop(Player player) {
        String rawTitle = MessageConfig.get("activity-reward.shop.title"); // e.g. "<#C1ADFE>Celestial Shop"
        Component title;
        try {
            title = mini.deserialize(rawTitle);
        } catch (Exception e) {
            title = legacy.deserialize(rawTitle);
        }

        Inventory gui = Bukkit.createInventory(null, 27, title);

        ConfigurationSection shopSection = plugin.getConfig().getConfigurationSection("activity-reward.shop.items");
        if (shopSection != null) {
            Set<String> keys = shopSection.getKeys(false);
            for (String key : keys) {
                ConfigurationSection itemSection = shopSection.getConfigurationSection(key);
                if (itemSection == null) continue;
                ItemStack item = buildItem(itemSection);
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot <= 26) {
                        gui.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
                ItemMeta fillerMeta = filler.getItemMeta();
                if (fillerMeta != null) {
                    fillerMeta.displayName(Component.text(" "));
                    filler.setItemMeta(fillerMeta);
                }
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    public void reloadShop(Player player) {
        openShop(player);
    }

    private ItemStack buildItem(ConfigurationSection section) {
        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String rawName = section.getString("name", "<#C1ADFE>Unnamed");
        Component name = deserializeFormatted(rawName);
        meta.displayName(name);

        List<String> rawLore = section.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String line : rawLore) {
            loreComponents.add(deserializeFormatted(line));
        }

        int cost = section.getInt("cost", 0);
        String costLineRaw = MessageConfig.get("activity-reward.shop.cost-line")
                .replace("%cost%", String.valueOf(cost));
        Component costLine = deserializeFormatted(costLineRaw);
        loreComponents.add(costLine);

        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    private Component deserializeFormatted(String raw) {
        try {
            return mini.deserialize(raw);
        } catch (Exception e) {
            return legacy.deserialize(raw);
        }
    }

    public void reload() {
        // Optional logic for reloading the shop state
    }
}
