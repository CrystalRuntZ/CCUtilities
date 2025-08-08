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

import java.util.*;

public class ConfirmationGuiManager {

    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public static final Map<UUID, PurchaseData> playerMetadata = new HashMap<>();

    public ConfirmationGuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int slot, ConfigurationSection itemSection, int balance) {
        String itemName = itemSection.getString("name", "<#C1ADFE>Unnamed");
        int cost = itemSection.getInt("cost");
        List<String> commands = itemSection.getStringList("commands");

        plugin.getLogger().info("[ActivityReward] " + player.getName() + " is viewing confirmation for " + itemName + " (Cost: " + cost + ", Balance: " + balance + ")");

        String titleRaw = MessageConfig.get("activity-reward.confirm.title");
        Component title = deserializeMini(titleRaw);

        Inventory gui = Bukkit.createInventory(null, 9, title);

        String cancelName = MessageConfig.get("activity-reward.confirm.cancel-name");
        String confirmName = MessageConfig.get("activity-reward.confirm.confirm-name");

        List<String> rawLore = MessageConfig.getStringList("activity-reward.confirm.info-lore");
        List<String> infoLore = new ArrayList<>();
        for (String line : rawLore) {
            infoLore.add(line.replace("%cost%", String.valueOf(cost)).replace("%bal%", String.valueOf(balance)));
        }

        gui.setItem(3, createPane(Material.RED_STAINED_GLASS_PANE, cancelName, Collections.emptyList()));
        gui.setItem(4, createPane(Material.WHITE_STAINED_GLASS_PANE, itemName, infoLore));
        gui.setItem(5, createPane(Material.LIME_STAINED_GLASS_PANE, confirmName, Collections.emptyList()));

        playerMetadata.put(player.getUniqueId(), new PurchaseData(slot, cost, commands));
        player.openInventory(gui);
    }

    private ItemStack createPane(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(deserializeMini(name));
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(deserializeMini(line));
            }
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component deserializeMini(String text) {
        try {
            return mini.deserialize(text);
        } catch (Exception e) {
            return legacy.deserialize(text);
        }
    }

    public record PurchaseData(int slot, int cost, List<String> commands) {}
}
