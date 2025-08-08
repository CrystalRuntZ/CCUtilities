package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class TunnelingEnchant implements CustomEnchant {

    private static final String LORE_LINE = "§7Tunneling";
    private static final Material FUEL = Material.COAL;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "tunneling";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.getType().toString().endsWith("_PICKAXE")) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        var lore = item.getItemMeta().lore();
        if (lore == null) return false;
        return lore.stream()
                .map(serializer::serialize)
                .anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return appliesTo(item);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // Not used
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return item;
    }

    @Override
    public String getLoreLine() {
        return "&7Tunneling";
    }

    public void onBlockBreak(Player player, Block block, ItemStack tool, BlockBreakEvent event) {
        if (!appliesTo(tool)) return;

        if (!org.celestialcraft.cCUtilities.utils.ClaimUtils.canBuild(player, block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (!consumeFuel(player)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        BlockFace direction = getFacingDirection(player);
        mine3x3Plane(block, direction, tool);
    }

    private boolean consumeFuel(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == FUEL && item.getAmount() > 0) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        player.sendMessage("§cYou need fuel (coal) to use the Tunneling enchant!");
        return false;
    }

    private void mine3x3Plane(Block origin, BlockFace direction, ItemStack tool) {
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Block target;

                switch (direction) {
                    case NORTH:
                    case SOUTH:
                        target = origin.getWorld().getBlockAt(ox + dx, oy + dy, oz);
                        break;
                    case EAST:
                    case WEST:
                        target = origin.getWorld().getBlockAt(ox, oy + dy, oz + dx);
                        break;
                    case UP:
                    case DOWN:
                        target = origin.getWorld().getBlockAt(ox + dx, oy, oz + dy);
                        break;
                    default:
                        continue;
                }

                if (canBreak(target)) {
                    target.breakNaturally(tool);
                }
            }
        }
    }

    private boolean canBreak(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && type != Material.BEDROCK
                && type != Material.OBSIDIAN
                && type != Material.REINFORCED_DEEPSLATE
                && type != Material.BARRIER
                && type.getHardness() >= 0;
    }

    private BlockFace getFacingDirection(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch < -45) return BlockFace.UP;
        if (pitch > 45) return BlockFace.DOWN;

        double yaw = (player.getLocation().getYaw() + 360) % 360;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }
}
