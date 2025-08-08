package org.celestialcraft.cCUtilities.modules.entitymanager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;

public class ChunkEntityTracker {

    public static List<Map.Entry<Chunk, Integer>> getTopChunks(int limit) {
        Map<Chunk, Integer> chunksWithCounts = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                int count = 0;
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getType() != EntityType.ITEM_FRAME) {
                        count++;
                    }
                }
                if (count > 0) {
                    chunksWithCounts.put(chunk, count);
                }
            }
        }

        return chunksWithCounts.entrySet().stream()
                .sorted(Map.Entry.<Chunk, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }
}
