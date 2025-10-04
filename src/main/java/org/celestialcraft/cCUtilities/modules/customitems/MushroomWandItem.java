package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MushroomWandItem implements CustomItem {

    private static final String LORE_LINE = "§7Mushroom Wand";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    private static final Set<Material> MUSHROOM_SUPPORTING_BLOCKS = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.MYCELIUM,
            Material.PODZOL,
            Material.NETHERRACK,
            Material.SOUL_SOIL,
            Material.STONE,
            Material.WARPED_NYLIUM,
            Material.CRIMSON_NYLIUM
    );

    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether");

    @Override
    public String getIdentifier() {
        return "mushroom_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        if (!ALLOWED_WORLDS.contains(player.getWorld().getName())) return;

        Block target = player.getTargetBlockExact(8);
        if (target == null) return;

        Block placeAt = target;
        Material targetType = target.getType();

        // Only support placement on mushroom-supporting blocks
        if (MUSHROOM_SUPPORTING_BLOCKS.contains(targetType)) {
            placeAt = target.getRelative(0, 1, 0);
        }

        if (placeAt.getType() != Material.AIR) return;

        // The block below placeAt must be a mushroom-supporting block as well
        if (!MUSHROOM_SUPPORTING_BLOCKS.contains(placeAt.getRelative(0, -1, 0).getType())) return;

        boolean red = ThreadLocalRandom.current().nextBoolean();
        TreeType type = red ? TreeType.RED_MUSHROOM : TreeType.BROWN_MUSHROOM;

        Location loc = placeAt.getLocation();

        boolean success = false;
        for (int i = 0; i < 10; i++) {
            if (placeAt.getWorld().generateTree(loc, ThreadLocalRandom.current(), type, (state) -> {})) {
                success = true;
                break;
            }
        }

        if (!success) {
            // Show action bar message to player
            player.sendActionBar(Component.text("§cFailed to grow a mushroom. Try a different location!"));
        }
    }
}
