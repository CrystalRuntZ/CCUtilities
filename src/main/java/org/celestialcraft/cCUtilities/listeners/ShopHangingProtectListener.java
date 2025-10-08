package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;

public class ShopHangingProtectListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    private boolean protectedEntity(Entity e) {
        return (e instanceof ItemFrame) || (e instanceof Painting) || (e instanceof ArmorStand);
    }

    private boolean canModify(Player p, Entity e) {
        ShopRegion region = ShopDataManager.getRegionAt(e.getLocation());
        if (region == null) return true; // not in a shop region
        boolean owner = p.getUniqueId().equals(ShopDataManager.getOwnerUUID(region.name()));
        boolean trusted = ShopDataManager.isTrusted(region.name(), p.getUniqueId());
        boolean bypass = p.hasPermission("shops.chest.bypass");
        return owner || trusted || bypass;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        Entity e = event.getRightClicked();
        if (!protectedEntity(e)) return;
        Player p = event.getPlayer();
        if (!canModify(p, e)) {
            event.setCancelled(true);
            p.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-permission-build")));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!(event.getDamager() instanceof Player p)) return;
        Entity e = event.getEntity();
        if (!protectedEntity(e)) return;
        if (!canModify(p, e)) {
            event.setCancelled(true);
            p.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-permission-build")));
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!(event.getRemover() instanceof Player p)) return;
        Entity e = event.getEntity();
        if (!protectedEntity(e)) return;
        if (!canModify(p, e)) {
            event.setCancelled(true);
            p.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-permission-build")));
        }
    }
}
