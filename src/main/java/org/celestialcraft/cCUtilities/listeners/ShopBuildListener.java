package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    // High priority: enforce the rule (cancel if NOT owner/trusted/bypass)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        enforce(event.getPlayer(), event /*sendMsgIfCancel=*/);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        enforce(event.getPlayer(), event /*sendMsgIfCancel=*/);
    }

    /**
     * Final pass: if someone else cancelled earlier but the player is actually allowed
     * (owner/trusted/bypass), un-cancel it so trusted players can build.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlaceEnsureAllowed(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        ensureAllowed(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakEnsureAllowed(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        ensureAllowed(event.getPlayer(), event);
    }

    private void enforce(Player player, BlockEvent event) {
        ShopRegion region = ShopDataManager.getRegionAt(event.getBlock().getLocation());
        if (region == null) return; // outside any shop

        if (!(event instanceof Cancellable c)) return;

        UUID uuid = player.getUniqueId();
        boolean isOwner   = region.name().equalsIgnoreCase(ShopDataManager.getClaim(uuid));
        boolean isTrusted = ShopDataManager.isTrusted(region.name(), uuid);
        boolean bypass    = player.hasPermission("shops.build.bypass") || player.hasPermission("shops.admin");

        if (!isOwner && !isTrusted && !bypass) {
            c.setCancelled(true);
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-permission-build")));
        }
        // If allowed, do nothing here — let it pass.
    }

    private void ensureAllowed(Player player, BlockEvent event) {
        if (!(event instanceof Cancellable c)) return;
        if (!c.isCancelled()) return; // nothing to fix

        ShopRegion region = ShopDataManager.getRegionAt(event.getBlock().getLocation());
        if (region == null) return; // outside any shop

        UUID uuid = player.getUniqueId();
        boolean isOwner   = region.name().equalsIgnoreCase(ShopDataManager.getClaim(uuid));
        boolean isTrusted = ShopDataManager.isTrusted(region.name(), uuid);
        boolean bypass    = player.hasPermission("shops.build.bypass") || player.hasPermission("shops.admin");

        // If our rules say they're allowed, un-cancel (silently)
        if (isOwner || isTrusted || bypass) {
            c.setCancelled(false);
        }
    }
}
