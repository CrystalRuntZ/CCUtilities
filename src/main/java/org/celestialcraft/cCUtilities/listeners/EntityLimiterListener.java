package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityLimitManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class EntityLimiterListener implements Listener {

    private final MiniMessage mini = MiniMessage.miniMessage();

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!ModuleManager.isEnabled("entitymanager")) return;

        var entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();
        String typeName = entity.getType().name();

        int totalEntities = chunk.getEntities().length;
        if (totalEntities >= EntityLimitManager.getTotalLimit()) {
            event.setCancelled(true);
            return;
        }

        Integer mobLimit = EntityLimitManager.getMobLimit(typeName);
        if (mobLimit != null) {
            long count = java.util.Arrays.stream(chunk.getEntities())
                    .filter(e -> e.getType().name().equalsIgnoreCase(typeName))
                    .count();
            if (count >= mobLimit) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!ModuleManager.isEnabled("entitymanager")) return;

        var entity = event.getEntity();
        var chunk = entity.getLocation().getChunk();
        var typeName = entity.getType().name();
        var player = event.getPlayer();

        int totalEntities = chunk.getEntities().length;
        if (totalEntities >= EntityLimitManager.getTotalLimit()) {
            event.setCancelled(true);
            if (player != null) {
                player.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.message-block-limit")));
            }
            return;
        }

        Integer mobLimit = EntityLimitManager.getMobLimit(typeName);
        if (mobLimit != null) {
            long count = java.util.Arrays.stream(chunk.getEntities())
                    .filter(e -> e.getType().name().equalsIgnoreCase(typeName))
                    .count();
            if (count >= mobLimit) {
                event.setCancelled(true);
                if (player != null) {
                    player.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.message-block-limit")));
                }
            }
        }
    }
}
