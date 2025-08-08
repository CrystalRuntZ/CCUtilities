package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopSelectionStorage;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.UUID;

public class ShopWandListener implements Listener {

    private boolean isShopWand(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_HOE) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        Component displayName = meta.displayName();
        return displayName != null && displayName.equals(Component.text("Shop Wand", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onWandClick(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!event.getPlayer().isOp()) return;
        if (!isShopWand(event.getItem())) return;
        if (event.getClickedBlock() == null) return;

        UUID uuid = event.getPlayer().getUniqueId();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos1(uuid, event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(Component.text("Position 1 set.", NamedTextColor.GRAY));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos2(uuid, event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(Component.text("Position 2 set.", NamedTextColor.GRAY));
        }

        if (ShopSelectionStorage.hasBoth(uuid)) {
            event.getPlayer().sendMessage(Component.text("Both positions set. Run ", NamedTextColor.GREEN)
                    .append(Component.text("/shops define <name>", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" to create the shop.", NamedTextColor.GREEN)));
        }
    }
}
