package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialShopManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ShopEditCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CelestialShopManager shopManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ShopEditCommand(JavaPlugin plugin, CelestialShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("activity-reward")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.player-only")));
            return true;
        }

        if (!player.hasPermission("celestialactivity.editshop")) {
            player.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.no-permission")));
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("editshop")) {
            player.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.usage-editshop")));
            return true;
        }

        int cost;
        try {
            cost = Integer.parseInt(args[1]);
            if (cost <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.invalid-cost")));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !item.hasItemMeta()) {
            player.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.invalid-item")));
            return true;
        }

        ConfigurationSection config = plugin.getConfig();
        ConfigurationSection shopSection = config.getConfigurationSection("activity-reward.shop.items");
        if (shopSection == null) {
            shopSection = config.createSection("activity-reward.shop.items");
        }

        Set<String> usedSlots = shopSection.getKeys(false);
        int slot = -1;
        for (int i = 0; i <= 26; i++) {
            if (!usedSlots.contains(String.valueOf(i))) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            player.sendMessage(mm.deserialize(MessageConfig.get("activity-reward.shop-full")));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.hasDisplayName() ? mm.serialize(Objects.requireNonNull(meta.displayName())) : "&fUnnamed";
        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            Objects.requireNonNull(meta.lore()).forEach(line -> lore.add(mm.serialize(line)));
        }

        ConfigurationSection newItemSection = shopSection.createSection(String.valueOf(slot));
        newItemSection.set("material", item.getType().name());
        newItemSection.set("name", displayName);
        newItemSection.set("lore", lore);
        newItemSection.set("cost", cost);
        newItemSection.set("commands", List.of("say %player% bought an item!"));

        plugin.saveConfig();

        player.sendMessage(mm.deserialize(
                MessageConfig.get("activity-reward.shop-item-added")
                        .replace("%slot%", String.valueOf(slot))
                        .replace("%cost%", String.valueOf(cost))
        ));

        shopManager.reloadShop(player);
        return true;
    }
}
