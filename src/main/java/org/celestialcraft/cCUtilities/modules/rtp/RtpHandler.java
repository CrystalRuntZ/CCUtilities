package org.celestialcraft.cCUtilities.modules.rtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class RtpHandler {
    private static final FileConfiguration config;

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

        // Overworld hardcap minY at 63
        int minY = type.equalsIgnoreCase("overworld")
                ? 63
                : config.getInt("rtp.worlds." + type + ".min-y", 0);

        int maxY = config.getInt("rtp.worlds." + type + ".max-y", world.getMaxHeight());

        Random random = new Random();

        for (int attempt = 0; attempt < 50; attempt++) {
            int x = getRandomInRange(-maxX, maxX);
            int z = getRandomInRange(-maxZ, maxZ);

            // Nether: cap maxY to 126, avoid lava, ensure space above
            if (type.equalsIgnoreCase("nether")) {
                int safeMaxY = Math.min(maxY, 126);
                int y = random.nextInt((safeMaxY - minY) + 1) + minY;

                Block block = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);
                Block twoAbove = world.getBlockAt(x, y + 2, z);

                if (isSafeBlockNether(block) && above.isEmpty() && twoAbove.isEmpty()) {
                    return new Location(world, x + 0.5, y + 1.0, z + 0.5);
                }
                continue;
            }

            // Overworld + End
            Integer y = findSafeY(world, x, z, minY, maxY, type);
            if (y != null) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        return null; // No safe location found
    }

    private static Integer findSafeY(World world, int x, int z, int minY, int maxY, String type) {
        for (int y = minY; y <= maxY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (type.equalsIgnoreCase("overworld") ? isSafeBlockOverworld(block) : isSafeBlockNether(block)) {
                Block above = world.getBlockAt(x, y + 1, z);
                Block twoAbove = world.getBlockAt(x, y + 2, z);
                if (above.isEmpty() && twoAbove.isEmpty()) {
                    return y + 1; // Stand on top of safe block
                }
            }
        }
        return null;
    }

    private static boolean isSafeBlockOverworld(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && type != Material.LAVA
                && type != Material.LAVA_CAULDRON
                && type != Material.WATER
                && type != Material.CACTUS
                && type != Material.FIRE;
    }

    private static boolean isSafeBlockNether(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && type != Material.LAVA
                && type != Material.LAVA_CAULDRON;
    }

    private static int getRandomInRange(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }
}
