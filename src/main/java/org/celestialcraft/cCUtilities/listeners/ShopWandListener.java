package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopSelectionStorage;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.UUID;

public class ShopWandListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    private boolean isShopWand(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_HOE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        Component dn = meta.displayName();
        return dn != null && dn.equals(Component.text("Shop Wand", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onWandClick(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (event.getClickedBlock() == null) return;

        Player p = event.getPlayer();
        if (!p.hasPermission("shops.define")) return;
        if (!isShopWand(event.getItem())) return;

        UUID uuid = p.getUniqueId();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos1(uuid, event.getClickedBlock().getLocation());
            p.sendMessage(mm.deserialize(MessageConfig.get("playershops.position-set-1")
                    .replace("%x%", String.valueOf(event.getClickedBlock().getX()))
                    .replace("%y%", String.valueOf(event.getClickedBlock().getY()))
                    .replace("%z%", String.valueOf(event.getClickedBlock().getZ()))));
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ShopSelectionStorage.setPos2(uuid, event.getClickedBlock().getLocation());
            p.sendMessage(mm.deserialize(MessageConfig.get("playershops.position-set-2")
                    .replace("%x%", String.valueOf(event.getClickedBlock().getX()))
                    .replace("%y%", String.valueOf(event.getClickedBlock().getY()))
                    .replace("%z%", String.valueOf(event.getClickedBlock().getZ()))));
            event.setCancelled(true);
        }

        if (ShopSelectionStorage.hasBoth(uuid)) {
            p.sendMessage(mm.deserialize("<green>Both positions set. Run <light_purple>/shops define <rank></light_purple> to create the shop."));
        }
    }
}
