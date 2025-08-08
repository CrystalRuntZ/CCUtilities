package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityLimitManager;

public class BlockLimitListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material type = event.getBlockPlaced().getType();
        Integer limit = EntityLimitManager.getBlockLimit(type);
        if (limit == null) return;

        Chunk chunk = event.getBlockPlaced().getChunk();
        long count = 0;

        for (BlockState tile : chunk.getTileEntities()) {
            if (tile.getType() == type) {
                count++;
            }
        }

        if (count >= limit) {
            event.setCancelled(true);
            String rawMessage = MessageConfig.get("entitymanager.block-limit-message");
            event.getPlayer().sendMessage(rawMessage.replace("&", "§").replace("<", "§").replace(">", ""));
        }
    }
}
