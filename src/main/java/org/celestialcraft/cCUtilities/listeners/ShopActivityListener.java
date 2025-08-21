package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.UUID;

public class ShopActivityListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        handleShopUpdate(event.getPlayer(), event.getBlock());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        handleShopUpdate(event.getPlayer(), event.getBlock());
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) return;

        handleShopUpdate(event.getPlayer(), block);
    }

    private void handleShopUpdate(Player player, Block block) {
        ShopRegion region = ShopDataManager.getRegionAt(block.getLocation());
        if (region == null) return;

        UUID uuid = player.getUniqueId();
        String ownedRegion = ShopDataManager.getClaim(uuid);
        boolean authorized = region.name().equalsIgnoreCase(ownedRegion) || ShopDataManager.isTrusted(region.name(), uuid);
        if (!authorized) return;

        ShopDataManager.setLastUpdated(region.name(), System.currentTimeMillis());
    }
}
