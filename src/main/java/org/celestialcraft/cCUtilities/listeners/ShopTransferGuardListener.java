package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopTransferGuardListener implements Listener {

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;

        InventoryHolder src = event.getSource().getHolder();
        InventoryHolder dst = event.getDestination().getHolder();

        if (ShopUtils.isShopChest(src) || ShopUtils.isShopChest(dst)) {
            event.setCancelled(true);
        }
    }
}
