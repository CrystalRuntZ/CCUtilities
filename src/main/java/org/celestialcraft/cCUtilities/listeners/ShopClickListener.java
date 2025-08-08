package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.ConfirmationGuiManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class ShopClickListener implements Listener {

    private final JavaPlugin plugin;
    private final CelestialPointManager pointManager;
    private final ConfirmationGuiManager confirmationGuiManager;
    private final Component shopTitle;

    public ShopClickListener(JavaPlugin plugin, CelestialPointManager pointManager, ConfirmationGuiManager confirmationGuiManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        this.confirmationGuiManager = confirmationGuiManager;
        this.shopTitle = MiniMessage.miniMessage().deserialize(MessageConfig.get("activity-reward.shop.title"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("celestialactivity")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(shopTitle)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("activity-reward.shop.items." + slot);
        if (section == null) return;

        int balance = pointManager.getPoints(player);
        confirmationGuiManager.open(player, slot, section, balance);
    }
}
