package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.*;

public class ArchaeologyShovelItem implements CustomItem {

    private static final String IDENTIFIER = "archaeology_shovel";
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final Random random = new Random();

    private static final Set<String> ALLOWED_WORLDS = new HashSet<>(Arrays.asList(
            "wild", "wild_nether"
    ));

    private static final Set<String> BLOCKED_WORLDS = new HashSet<>(Arrays.asList(
            "spawnworld", "shops"
    ));

    private static final Set<Material> TARGET_BLOCKS = EnumSet.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.CLAY
    );

    private static final List<Material> POSSIBLE_LOOT = Arrays.asList(
            Material.BURN_POTTERY_SHERD,
            Material.DANGER_POTTERY_SHERD,
            Material.EXPLORER_POTTERY_SHERD,
            Material.FRIEND_POTTERY_SHERD,
            Material.HEART_POTTERY_SHERD,
            Material.HOWL_POTTERY_SHERD,
            Material.MINER_POTTERY_SHERD,
            Material.MOURNER_POTTERY_SHERD,
            Material.PLENTY_POTTERY_SHERD,
            Material.PRIZE_POTTERY_SHERD,
            Material.SHEAF_POTTERY_SHERD,
            Material.SHELTER_POTTERY_SHERD,
            Material.SKULL_POTTERY_SHERD,
            Material.SNORT_POTTERY_SHERD,
            Material.BONE,
            Material.BONE_BLOCK
    );

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore() || meta.lore() == null) return false;

        for (Component line : Objects.requireNonNull(meta.lore())) {
            if ("§7Archaeology Shovel".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBlockBreak(Player player, Block block, ItemStack item, BlockBreakEvent event) {
        if (!TARGET_BLOCKS.contains(block.getType())) return;

        String worldName = block.getWorld().getName();
        // Only proceed if in allowed worlds
        if (!ALLOWED_WORLDS.contains(worldName)) return;
        // Explicitly block forbidden worlds just in case
        if (BLOCKED_WORLDS.contains(worldName)) return;

        if (!ClaimUtils.canBuild(player, block.getLocation())) return;

        if (Math.random() <= 0.10) {
            Material randomDrop = POSSIBLE_LOOT.get(random.nextInt(POSSIBLE_LOOT.size()));
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(randomDrop));
        }
    }
}
