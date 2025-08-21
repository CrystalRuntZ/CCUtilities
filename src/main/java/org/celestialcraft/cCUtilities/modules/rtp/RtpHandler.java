package org.celestialcraft.cCUtilities.modules.rtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class RtpHandler {
    private static final FileConfiguration config;
    private static final Random RNG = new Random();

    static {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CCUtilities");
        if (plugin == null) throw new IllegalStateException("CCUtilities plugin not found");
        config = plugin.getConfig();
    }

    public static Location findSafeLocation(String type) {
        String worldName = config.getString("rtp.worlds." + type + ".name");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        int maxX = config.getInt("rtp.worlds." + type + ".max-x");
        int maxZ = config.getInt("rtp.worlds." + type + ".max-z");

        // Overworld hardcap minY at 63 to avoid caves/ocean
        int minY = type.equalsIgnoreCase("overworld")
                ? 63
                : config.getInt("rtp.worlds." + type + ".min-y", world.getMinHeight());

        int maxY = config.getInt("rtp.worlds." + type + ".max-y", world.getMaxHeight());

        // Try more times for better success, especially in Nether
        for (int attempt = 0; attempt < 150; attempt++) {
            int x = getRandomInRange(-maxX, maxX);
            int z = getRandomInRange(-maxZ, maxZ);

            if (type.equalsIgnoreCase("nether")) {
                // Avoid the nether roof (bedrock at ~127). Cap search at 126.
                int top = Math.min(maxY, 126);
                Integer y = findSafeYInNether(world, x, z, Math.max(minY, world.getMinHeight()), top);
                if (y != null) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
                continue;
            }

            // Overworld + End: find first safe standing spot with headroom
            Integer y = findSafeY(world, x, z, minY, maxY, type);
            if (y != null) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        return null; // No safe location found
    }

    private static Integer findSafeYInNether(World world, int x, int z, int minY, int maxY) {
        // Scan DOWN from maxY (<=126) to find a solid floor that is not lava, with two air blocks above.
        for (int y = maxY; y >= minY; y--) {
            Block floor = world.getBlockAt(x, y, z);
            Material ft = floor.getType();

            // Must be solid to stand on
            if (!ft.isSolid()) continue;

            // Avoid lava and lava cauldron floors
            if (ft == Material.LAVA || ft == Material.LAVA_CAULDRON) continue;

            // Extra guard to avoid roof plateau (standing on bedrock near the roof)
            // If the floor is bedrock at very high Y, skip it to avoid "roof-like" placements.
            if (ft == Material.BEDROCK && y >= 123) continue;

            Block head = world.getBlockAt(x, y + 1, z);
            Block aboveHead = world.getBlockAt(x, y + 2, z);

            // Need two air spaces to avoid suffocation
            if (head.isEmpty() && aboveHead.isEmpty()) {
                return y + 1; // stand on top of floor
            }
        }
        return null;
    }

    private static Integer findSafeY(World world, int x, int z, int minY, int maxY, String type) {
        for (int y = minY; y <= maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            boolean goodFloor = type.equalsIgnoreCase("overworld")
                    ? isSafeBlockOverworld(block)
                    : isSafeBlockNether(block);

            if (goodFloor) {
                Block above = world.getBlockAt(x, y + 1, z);
                Block twoAbove = world.getBlockAt(x, y + 2, z);
                if (above.isEmpty() && twoAbove.isEmpty()) {
                    return y + 1; // stand on top
                }
            }
        }
        return null;
    }

    private static boolean isSafeBlockOverworld(Block block) {
        Material t = block.getType();
        return t.isSolid()
                && t != Material.LAVA
                && t != Material.LAVA_CAULDRON
                && t != Material.WATER
                && t != Material.CACTUS
                && t != Material.FIRE
                && t != Material.CAMPFIRE
                && t != Material.SOUL_CAMPFIRE;
    }

    private static boolean isSafeBlockNether(Block block) {
        Material t = block.getType();
        return t.isSolid()
                && t != Material.LAVA
                && t != Material.LAVA_CAULDRON;
    }

    private static int getRandomInRange(int min, int max) {
        return RNG.nextInt(max - min + 1) + min;
    }
}
