package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.orewatcher.NotificationTracker;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;

public class OreMineListener implements Listener {

    private final NotificationTracker tracked = new NotificationTracker();
    private final Set<Material> watchBlocks;
    private final int radius;
    private final long cooldown;
    private final String permission;

    public OreMineListener(FileConfiguration config) {
        this.watchBlocks = new HashSet<>();
        for (String mat : config.getStringList("orewatcher.blocks")) {
            Material material = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
            if (material != null) watchBlocks.add(material);
        }
        this.radius = config.getInt("orewatcher.radius", 8);
        this.cooldown = config.getLong("orewatcher.cooldown-seconds", 5) * 1000;
        this.permission = config.getString("orewatcher.notify-permission", "OreWatcher.notify");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("orewatcher")) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!watchBlocks.contains(block.getType())) return;
        if (tracked.isOnCooldown(player.getUniqueId())) return;

        Set<Block> connected = findConnected(block, new HashSet<>(), radius);
        tracked.setCooldown(player.getUniqueId(), cooldown);

        String raw = MessageConfig.get("orewatcher.message")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(connected.size()))
                .replace("%block%", block.getType().name())
                .replace("%x%", String.valueOf(block.getX()))
                .replace("%y%", String.valueOf(block.getY()))
                .replace("%z%", String.valueOf(block.getZ()));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) {
                p.sendMessage(MiniMessage.miniMessage().deserialize(raw));
            }
        }
    }

    private Set<Block> findConnected(Block start, Set<Block> visited, int radius) {
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Block nearby = current.getRelative(dx, dy, dz);
                        if (!visited.contains(nearby)
                                && current.getLocation().distance(nearby.getLocation()) <= radius
                                && nearby.getType() == current.getType()) {
                            visited.add(nearby);
                            queue.add(nearby);
                        }
                    }
                }
            }
        }

        return visited;
    }
}
