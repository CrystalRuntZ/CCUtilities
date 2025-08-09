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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MushroomWandItem implements CustomItem {

    private static final String LORE_LINE = "§7Mushroom Wand";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

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

        Block target = player.getTargetBlockExact(8);
        if (target == null) return;

        Block placeAt = target;
        Material t = target.getType();
        if (t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.MYCELIUM) {
            placeAt = target.getRelative(0, 1, 0);
        }

        if (placeAt.getType() != Material.AIR) return;
        if (placeAt.getRelative(0, -1, 0).getType() != Material.GRASS_BLOCK) return;

        boolean red = ThreadLocalRandom.current().nextBoolean();
        TreeType type = red ? TreeType.RED_MUSHROOM : TreeType.BROWN_MUSHROOM;

        Location loc = placeAt.getLocation();
        Random rng = ThreadLocalRandom.current();

        for (int i = 0; i < 10; i++) {
            boolean success = placeAt.getWorld().generateTree(
                    loc,
                    rng,
                    type,
                    (state) -> {} // no-op consumer; you could tweak states here if you want
            );
            if (success) break;
        }
    }
}
