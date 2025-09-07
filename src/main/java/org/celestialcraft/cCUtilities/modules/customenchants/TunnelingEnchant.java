package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;
import org.celestialcraft.cCUtilities.util.MiningPrecedence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TunnelingEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Tunneling";
    private static final Material FUEL = Material.COAL;
    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether", "wild_the_end");

    @Override public String getIdentifier() { return "tunneling"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType().toString().endsWith("_PICKAXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        // LoreUtil handles both Component and legacy lore (&/§)
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* not used */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!appliesTo(tool) || !hasEnchant(tool)) return;

        Block origin = event.getBlock();

        // World gate
        World world = origin.getWorld();
        if (!ALLOWED_WORLDS.contains(world.getName())) return;

        // Claim check
        if (!org.celestialcraft.cCUtilities.utils.ClaimUtils.canBuild(player, origin.getLocation())) {
            event.setCancelled(true);
            return;
        }

        // If the origin block doesn't prefer a pickaxe, let vanilla handle it.
        if (MiningPrecedence.preferredTool(origin) != MiningPrecedence.ToolClass.PICKAXE) return;

        // Only tunnel if the current tool is appropriate for the origin block.
        if (!MiningPrecedence.isPreferredTool(origin, tool)) return;

        // Build and filter the 3x3 plane BEFORE consuming fuel.
        BlockFace face = getFacingDirection(player);
        List<Block> targets = collect3x3(origin, face);

        List<Block> mineable = new ArrayList<>(9);
        for (Block b : targets) {
            if (b == null) continue;
            // Skip quick unbreakables/forbidden blocks
            if (MiningPrecedence.isUnbreakableOrShouldSkip(b)) continue;

            Material type = b.getType();
            if (type.isAir() || !type.isSolid()) continue;

            // Only break blocks where this tool class is preferred
            if (!MiningPrecedence.isPreferredTool(b, tool)) continue;

            mineable.add(b);
        }

        // If nothing valid to mine, let vanilla proceed (don’t cancel and don’t spend fuel)
        if (mineable.isEmpty()) return;

        // Fuel gate (only spend coal if we will mine something)
        if (!consumeFuel(player)) {
            event.setCancelled(true);
            return;
        }

        // Cancel vanilla break; we’ll break the filtered 3x3
        event.setCancelled(true);

        for (Block b : mineable) {
            // breakNaturally respects tool enchants (silk/fortune) and loot tables
            b.breakNaturally(tool);
        }
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

    private List<Block> collect3x3(Block origin, BlockFace direction) {
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        List<Block> out = new ArrayList<>(9);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Block target = switch (direction) {
                    case NORTH, SOUTH -> origin.getWorld().getBlockAt(ox + dx, oy + dy, oz);
                    case EAST, WEST -> origin.getWorld().getBlockAt(ox, oy + dy, oz + dx);
                    case UP, DOWN -> origin.getWorld().getBlockAt(ox + dx, oy, oz + dy);
                    default -> null;
                };
                out.add(target);
            }
        }
        return out;
    }

    private BlockFace getFacingDirection(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch < -45) return BlockFace.UP;
        if (pitch > 45)  return BlockFace.DOWN;

        double yaw = (player.getLocation().getYaw() + 360.0) % 360.0;
        if (yaw >= 45 && yaw < 135)   return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225)  return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315)  return BlockFace.EAST;
        return BlockFace.SOUTH;
    }
}
