package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopChestDragListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!ShopUtils.isShopChest(holder)) return;

        Sign sign = ShopUtils.getAttachedSign(holder);
        if (sign == null) return;

        Component line = sign.getSide(Side.FRONT).line(3);
        String owner = PLAIN.serialize(line).trim();

        boolean ownerOrBypass = player.getName().equalsIgnoreCase(owner) || player.hasPermission("shops.chest.bypass");
        if (!ownerOrBypass) {
            event.setCancelled(true);
        }
    }
}
