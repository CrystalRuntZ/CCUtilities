package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BlackstonePickaxeItem implements CustomItem {

    private static final String LORE = "§7Blackstone Pickaxe";

    private static final Set<Material> BLACKSTONE_VARIANTS = Set.of(
            Material.BLACKSTONE,
            Material.POLISHED_BLACKSTONE,
            Material.POLISHED_BLACKSTONE_BRICKS,
            Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Material.GILDED_BLACKSTONE
    );

    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether");

    @Override
    public String getIdentifier() {
        return "blackstone_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String name = item.getType().name();
        if (!name.endsWith("_PICKAXE")) return false;
        return LoreUtil.itemHasLore(item, LORE);
    }

    @Override
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        // Not used for this item
    }

    @Override
    public void onAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        // No attack behavior
    }

    public void onBlockBreak(Player player, Block block, ItemStack item, BlockBreakEvent event) {
        if (block.getType() != Material.STONE) return;
        if (!ALLOWED_WORLDS.contains(block.getWorld().getName())) return;
        if (!matches(item)) return;

        // Suppress default drops and XP
        event.setDropItems(false);
        event.setExpToDrop(0);

        // DO NOT cancel the event to allow block break to proceed naturally
        // event.setCancelled(true); // Removed to prevent double drops and block break issues

        // Drop exactly one random blackstone variant
        int index = ThreadLocalRandom.current().nextInt(BLACKSTONE_VARIANTS.size());
        Material chosen = BLACKSTONE_VARIANTS.stream().skip(index).findFirst().orElse(Material.BLACKSTONE);
        ItemStack drop = new ItemStack(chosen);

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(dropLoc, drop);

        // Play sound effect with slight pitch variation
        float pitch = 1.2f + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.1f;
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.6f, pitch);
    }
}
