package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MapArtProtectionListener implements Listener {
    private final MapArtDataManager data;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public MapArtProtectionListener(MapArtDataManager data) {
        this.data = data;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        var region = data.regionAt(e.getBlockPlaced().getLocation());
        if (region == null) return;
        String name = region.getName();
        var p = e.getPlayer();
        if (!data.isClaimed(name)) return;
        if (p.hasPermission("maparts.admin")) return;
        if (data.isOwner(name, p.getUniqueId())) return;
        if (data.isTrusted(name, p.getUniqueId())) return;
        e.setCancelled(true);
        p.sendMessage(mini.deserialize("<red>You cannot build in this mapart.</red>"));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        var region = data.regionAt(e.getBlock().getLocation());
        if (region == null) return;
        String name = region.getName();
        var p = e.getPlayer();
        if (!data.isClaimed(name)) return;
        if (p.hasPermission("maparts.admin")) return;
        if (data.isOwner(name, p.getUniqueId())) return;
        if (data.isTrusted(name, p.getUniqueId())) return;
        e.setCancelled(true);
        p.sendMessage(mini.deserialize("<red>You cannot build in this mapart.</red>"));
    }

    @EventHandler
    public void onUseEmptyMap(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        if (e.getItem().getType() != Material.MAP) return;
        if (e.getClickedBlock() == null && e.getInteractionPoint() == null && e.getAction().isLeftClick()) return;
        var region = data.regionAt(e.getPlayer().getLocation());
        if (region == null) return;
        String name = region.getName();
        var p = e.getPlayer();
        if (!data.isClaimed(name)) return;
        if (p.hasPermission("maparts.admin")) return;
        if (data.isOwner(name, p.getUniqueId())) return;
        if (data.isTrusted(name, p.getUniqueId())) return;
        e.setCancelled(true);
        p.sendMessage(mini.deserialize("<red>You cannot create a map on this mapart.</red>"));
    }
}
