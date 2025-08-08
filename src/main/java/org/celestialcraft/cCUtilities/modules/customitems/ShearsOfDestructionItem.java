package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ShearsOfDestructionItem implements CustomItem {

    private static final String RAW_LORE = "&7Shears of Destruction";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    private static final Set<Material> INSTANT_BREAK_BLOCKS = EnumSet.of(
            Material.COBWEB,
            Material.VINE,
            Material.GLOW_LICHEN,
            Material.GLASS,
            Material.TINTED_GLASS,
            Material.WHITE_STAINED_GLASS,
            Material.ORANGE_STAINED_GLASS,
            Material.MAGENTA_STAINED_GLASS,
            Material.LIGHT_BLUE_STAINED_GLASS,
            Material.YELLOW_STAINED_GLASS,
            Material.LIME_STAINED_GLASS,
            Material.PINK_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS,
            Material.LIGHT_GRAY_STAINED_GLASS,
            Material.CYAN_STAINED_GLASS,
            Material.PURPLE_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS,
            Material.BROWN_STAINED_GLASS,
            Material.GREEN_STAINED_GLASS,
            Material.RED_STAINED_GLASS,
            Material.BLACK_STAINED_GLASS
    );

    @Override
    public String getIdentifier() {
        return "shears_of_destruction";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.SHEARS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase("spawnworld")) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!ClaimUtils.canBuild(player, block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        Material type = block.getType();
        if (!type.name().endsWith("_LEAVES") && !INSTANT_BREAK_BLOCKS.contains(type)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!matches(tool)) return;

        boolean silk = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        event.setCancelled(true);
        block.setType(Material.AIR);

        if (silk) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(type));
        } else {
            for (ItemStack drop : block.getDrops(tool)) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    @Override
    public void onBlockBreak(Player player, Block block, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;

        if (player.getWorld().getName().equalsIgnoreCase("spawnworld")) {
            event.setCancelled(true);
            return;
        }

        if (!ClaimUtils.canBuild(player, block.getLocation())) {
            event.setCancelled(true);
        }
    }
}
