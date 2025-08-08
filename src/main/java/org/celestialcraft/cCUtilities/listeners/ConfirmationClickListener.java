package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialPointManager;
import org.celestialcraft.cCUtilities.modules.activity.ConfirmationGuiManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfirmationClickListener implements Listener {

    private final JavaPlugin plugin;
    private final CelestialPointManager pointManager;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final Component confirmTitle = mini.deserialize("<white>Confirm Purchase");
    private final File logFile;

    public ConfirmationClickListener(JavaPlugin plugin, CelestialPointManager pointManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        this.logFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), "logs/activity-shop.log");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("activity")) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(confirmTitle)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        switch (clicked.getType()) {
            case LIME_STAINED_GLASS_PANE -> {
                ConfirmationGuiManager.PurchaseData data = ConfirmationGuiManager.playerMetadata.get(player.getUniqueId());
                if (data == null) return;

                int balance = pointManager.getPoints(player);
                if (balance < data.cost()) {
                    String msg = MessageConfig.get("activity-reward.shop.confirm-gui.insufficient-points")
                            .replace("%cost%", String.valueOf(data.cost()))
                            .replace("%player%", player.getName());
                    player.sendMessage(legacy.deserialize(msg));
                    return;
                }

                pointManager.addPoints(player, -data.cost());
                for (String cmd : data.commands()) {
                    String parsed = cmd.replace("%player%", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
                }

                String successMsg = MessageConfig.get("activity-reward.shop.confirm-gui.success-message")
                        .replace("%cost%", String.valueOf(data.cost()))
                        .replace("%player%", player.getName());
                player.sendMessage(legacy.deserialize(successMsg));

                if (plugin.getConfig().getBoolean("activity-reward.confirm.effect.success", false)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, true, true));
                }

                logPurchase(player.getName(), data.slot(), data.cost(), data.commands());
                player.closeInventory();
            }

            case RED_STAINED_GLASS_PANE -> {
                String cancelMsg = MessageConfig.get("activity-reward.shop.confirm-gui.cancel-message")
                        .replace("%player%", player.getName());
                player.sendMessage(legacy.deserialize(cancelMsg));

                if (plugin.getConfig().getBoolean("activity-reward.confirm.effect.cancel", false)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, true, true, true));
                }

                player.closeInventory();
            }

            default -> {}
        }
    }

    private void logPurchase(String playerName, int slot, int cost, List<String> commands) {
        try {
            if (!logFile.exists()) {
                File parent = logFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    plugin.getLogger().warning("Failed to create directory for activity-shop.log");
                    return;
                }
                if (!logFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create activity-shop.log");
                    return;
                }
            }

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String commandStr = String.join(" | ", commands);
            String line = "[" + time + "] " + playerName + " purchased slot " + slot + " for " + cost + " points -> " + commandStr + "\n";

            java.nio.file.Files.write(logFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to activity-shop.log: " + e.getMessage());
        }
    }
}
