package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.Cancellable;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.UUID;

public class ShopBuildListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        handle(event.getPlayer(), event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        handle(event.getPlayer(), event);
    }

    private void handle(Player player, BlockEvent event) {
        ShopRegion region = ShopDataManager.getRegionAt(event.getBlock().getLocation());
        if (region == null) return;

        UUID uuid = player.getUniqueId();
        String ownedRegion = ShopDataManager.getClaim(uuid);
        boolean isOwner = region.name().equals(ownedRegion);
        boolean isTrusted = ShopDataManager.getTrusted(region.name()).contains(uuid);

        if (!isOwner && !isTrusted) {
            if (event instanceof Cancellable) {
                ((Cancellable) event).setCancelled(true);
            }
            String msg = MessageConfig.get("playershops.message-no-permission-build");
            player.sendMessage(mm.deserialize(msg));
        }
    }
}
