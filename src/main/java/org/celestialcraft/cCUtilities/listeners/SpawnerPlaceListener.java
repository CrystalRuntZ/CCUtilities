package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.utils.SpawnerDataUtil;

public class SpawnerPlaceListener implements Listener {

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;

        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof CreatureSpawner spawner)) return;

        var type = SpawnerDataUtil.getSpawnerType(item);
        if (type != null) {
            spawner.setSpawnedType(type);
            spawner.update();
        }
    }
}
