package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapArtEntryLockListener implements Listener {
    private final MapArtDataManager data;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<UUID, Long> lastNotify = new HashMap<>();

    public MapArtEntryLockListener(MapArtDataManager data) {
        this.data = data;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        e.getFrom();
        e.getTo();
        var from = data.regionAt(e.getFrom());
        var to = data.regionAt(e.getTo());
        if (to == null) return;
        if (from != null && from.getName().equalsIgnoreCase(to.getName())) return;
        String name = to.getName();
        if (!data.isClaimed(name)) return;
        if (!data.isLocked(name)) return;
        var p = e.getPlayer();
        if (p.hasPermission("maparts.admin")) return;
        if (data.isOwner(name, p.getUniqueId())) return;
        if (data.isTrusted(name, p.getUniqueId())) return;
        e.setTo(e.getFrom());
        long now = System.currentTimeMillis();
        Long last = lastNotify.get(p.getUniqueId());
        if (last == null || now - last > 1500) {
            p.sendMessage(mini.deserialize("<red>This mapart is locked.</red>"));
            lastNotify.put(p.getUniqueId(), now);
        }
    }
}
