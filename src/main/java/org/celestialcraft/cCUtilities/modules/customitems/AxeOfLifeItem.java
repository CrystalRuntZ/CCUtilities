package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AxeOfLifeItem implements CustomItem {

    private static final String LORE_LINE = "§7Axe of Life";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether");

    private static final Map<Material, Material> SAPLING_MAP = Map.ofEntries(
            Map.entry(Material.OAK_LOG, Material.OAK_SAPLING),
            Map.entry(Material.SPRUCE_LOG, Material.SPRUCE_SAPLING),
            Map.entry(Material.BIRCH_LOG, Material.BIRCH_SAPLING),
            Map.entry(Material.JUNGLE_LOG, Material.JUNGLE_SAPLING),
            Map.entry(Material.ACACIA_LOG, Material.ACACIA_SAPLING),
            Map.entry(Material.DARK_OAK_LOG, Material.DARK_OAK_SAPLING),
            Map.entry(Material.CHERRY_LOG, Material.CHERRY_SAPLING),
            Map.entry(Material.MANGROVE_LOG, Material.MANGROVE_PROPAGULE),
            Map.entry(Material.CRIMSON_STEM, Material.CRIMSON_FUNGUS),
            Map.entry(Material.WARPED_STEM, Material.WARPED_FUNGUS)
    );

    @Override
    public String getIdentifier() {
        return "axe_of_life";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("_AXE") || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> serializer.serialize(line).equals(LORE_LINE));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        if (!ALLOWED_WORLDS.contains(event.getBlock().getWorld().getName())) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!matches(tool)) return;

        Block brokenBlock = event.getBlock();
        Material logType = brokenBlock.getType();
        if (!SAPLING_MAP.containsKey(logType)) return;

        Material sapling = SAPLING_MAP.get(logType);

        new BukkitRunnable() {
            @Override
            public void run() {
                Block below = brokenBlock.getLocation().subtract(0, 1, 0).getBlock();
                Block target = brokenBlock.getLocation().getBlock();

                boolean isDirtBased = switch (sapling) {
                    case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING,
                         ACACIA_SAPLING, DARK_OAK_SAPLING, CHERRY_SAPLING, MANGROVE_PROPAGULE ->
                            below.getType() == Material.DIRT || below.getType() == Material.GRASS_BLOCK;
                    default -> false;
                };

                boolean isNetherFungus = switch (sapling) {
                    case CRIMSON_FUNGUS -> below.getType() == Material.CRIMSON_NYLIUM;
                    case WARPED_FUNGUS -> below.getType() == Material.WARPED_NYLIUM;
                    default -> false;
                };

                if (isDirtBased || isNetherFungus) {
                    target.setType(sapling);
                }
            }
        }.runTaskLater(CCUtilities.getInstance(), 1L);
    }
}
