package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopBlockProtectListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // If breaking a container that is a shop chest
        if (block.getState() instanceof Container) {
            if (ShopUtils.isShopChest(((Container) block.getState()).getInventory().getHolder())) {
                Sign sign = ShopUtils.getAttachedSign(((Container) block.getState()).getInventory().getHolder());
                if (sign != null) {
                    String owner = PLAIN.serialize(sign.getSide(Side.FRONT).line(3)).trim();
                    boolean allowed = player.getName().equalsIgnoreCase(owner) || player.hasPermission("shops.chest.bypass");
                    if (!allowed) {
                        event.setCancelled(true);
                        player.sendMessage(MessageConfig.mm(MessageConfig.get("playershops.message-not-owner")));
                    }
                }
            }
            return;
        }

        // If breaking a sign that is a shop sign ([PRICE]), protect it
        if (block.getState() instanceof Sign sign) {
            String first = PLAIN.serialize(sign.getSide(Side.FRONT).line(0)).trim();
            if ("[PRICE]".equalsIgnoreCase(first)) {
                String owner = PLAIN.serialize(sign.getSide(Side.FRONT).line(3)).trim();
                boolean allowed = player.getName().equalsIgnoreCase(owner) || player.hasPermission("shops.chest.bypass");
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendMessage(MessageConfig.mm(MessageConfig.get("playershops.message-not-owner")));
                }
            }
        }
    }
}
