package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.PlayerShopsModule;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopSelectionStorage;

public class ShopSelectionListener implements Listener {

    private final PlayerShopsModule module;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ShopSelectionListener(PlayerShopsModule module) {
        this.module = module;
    }

    @EventHandler
    public void onSelect(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != module.getSelectionItem() || !player.hasPermission("shops.define")) return;

        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos1(player.getUniqueId(), loc);
            String msg = MessageConfig.get("playershops.position-set-1")
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));
            player.sendMessage(mm.deserialize(msg));
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos2(player.getUniqueId(), loc);
            String msg = MessageConfig.get("playershops.position-set-2")
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));
            player.sendMessage(mm.deserialize(msg));
            event.setCancelled(true);
        }
    }
}
