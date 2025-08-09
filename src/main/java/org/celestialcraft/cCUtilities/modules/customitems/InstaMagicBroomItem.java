package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InstaMagicBroomItem implements CustomItem {

    private static final String LORE_LINE = "§7Insta Magic Broom";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    // Safety cap so someone doesn’t nuke a whole beach by accident; tweak if you want
    private static final int MAX_BLOCKS = 4096;

    // 26-adjacent neighbor offsets (radius 1 cube minus center), matching Skript's "loop blocks in radius 1"
    private static final int[] OFF = {-1, 0, 1};
    private static final int[][] NEIGHBORS;
    static {
        List<int[]> list = new ArrayList<>(26);
        for (int dx : OFF) for (int dy : OFF) for (int dz : OFF) {
            if (dx == 0 && dy == 0 && dz == 0) continue;
            list.add(new int[]{dx, dy, dz});
        }
        NEIGHBORS = list.toArray(new int[0][]);
    }

    @Override
    public String getIdentifier() {
        return "insta_magic_broom";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onBlockBreak(Player player, Block origin, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;
        if (origin.getType() != Material.GRAVEL) return;

        // We’re taking over the break; no default drops, just like the Skript.
        event.setCancelled(true);

        World world = origin.getWorld();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(origin);

        // Track visited blocks using a packed long (x,y,z) to keep it fast.
        HashSet<Long> visited = new HashSet<>();
        visited.add(pack(origin.getX(), origin.getY(), origin.getZ()));

        int processed = 0;
        while (!queue.isEmpty() && processed < MAX_BLOCKS) {
            Block b = queue.poll();
            if (b.getType() != Material.GRAVEL) continue;

            // Remove block (no drops)
            b.setType(Material.AIR, false);
            processed++;

            // Enqueue neighbors if gravel
            final int bx = b.getX(), by = b.getY(), bz = b.getZ();
            for (int[] n : NEIGHBORS) {
                int nx = bx + n[0], ny = by + n[1], nz = bz + n[2];
                if (ny < world.getMinHeight() || ny >= world.getMaxHeight()) continue;
                long key = pack(nx, ny, nz);
                if (visited.add(key)) {
                    Block nb = world.getBlockAt(nx, ny, nz);
                    if (nb.getType() == Material.GRAVEL) {
                        queue.add(nb);
                    }
                }
            }
        }
    }

    private static long pack(int x, int y, int z) {
        // pack 3 ints into a long (21 bits x/z, 22 bits y) – good enough for vanilla ranges
        return ((long)(x & 0x1FFFFF) << 43) | ((long)(y & 0x3FFFFF) << 21) | (long)(z & 0x1FFFFF);
    }
}
