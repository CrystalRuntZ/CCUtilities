package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityLimitManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ModuleManager.isEnabled("entitymanager")) return;

        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        Material material = block.getType();

        Integer limit = EntityLimitManager.getBlockLimit(material);
        if (limit == null) return;

        int count = countMatchingBlocksInChunk(chunk, material);
        if (count >= limit) {
            event.setCancelled(true);

            String raw = MessageConfig.get("entitymanager.block-limit-message")
                    .replace("<block>", material.name())
                    .replace("<limit>", limit.toString());

            Player player = event.getPlayer();
            player.sendMessage(MiniMessage.miniMessage().deserialize(raw));
        }
    }

    private int countMatchingBlocksInChunk(Chunk chunk, Material material) {
        int count = 0;
        int minY = -64;
        int maxY = 320;

        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z).getType() == material) {
                        count++;
                        if (count > 40) return count;
                    }
                }
            }
        }
        return count;
    }
}
