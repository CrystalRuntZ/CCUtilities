package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopSignProtectionListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!(event.getBlock().getState() instanceof Sign oldSign)) return;

        // Was this already a shop sign before the edit?
        String oldL0 = PLAIN.serialize(oldSign.getSide(Side.FRONT).line(0)).trim();
        if (!"[PRICE]".equalsIgnoreCase(oldL0)) return;

        String owner = PLAIN.serialize(oldSign.getSide(Side.FRONT).line(3)).trim();
        boolean allowed = player.hasPermission("shops.chest.bypass")
                || player.getName().equalsIgnoreCase(owner);
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        // Only guard shop signs
        String l0 = PLAIN.serialize(sign.getSide(Side.FRONT).line(0)).trim();
        if (!"[PRICE]".equalsIgnoreCase(l0)) return;

        // Ensure sign is actually attached to a shop container
        InventoryHolder holder = ShopUtils.findAttachedContainer(sign.getBlock());
        if (!ShopUtils.isShopChest(holder)) return;

        String owner = PLAIN.serialize(sign.getSide(Side.FRONT).line(3)).trim();
        boolean allowed = player.hasPermission("shops.chest.bypass")
                || player.getName().equalsIgnoreCase(owner);
        if (!allowed) {
            event.setCancelled(true);
        }
    }
}
