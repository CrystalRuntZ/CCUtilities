package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LavaforgedBootsItem implements CustomItem {

    private static final String LORE_LINE = "&7Lavaforged Boots";
    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether");

    private final Map<UUID, Set<Location>> transformedBlocks = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "lavaforged_boots";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_BOOTS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                Component.text(LORE_LINE.replace("&", "§")).equals(component));
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!ALLOWED_WORLDS.contains(player.getWorld().getName().toLowerCase())) return;

        ItemStack boots = player.getInventory().getItem(EquipmentSlot.FEET);
        if (!matches(boots)) return;

        UUID uuid = player.getUniqueId();
        Location playerLoc = player.getLocation();
        Set<Location> changed = transformedBlocks.computeIfAbsent(uuid, k -> new HashSet<>());

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Location checkLoc = playerLoc.clone().add(dx, dy - 2, dz);
                    Block block = checkLoc.getBlock();
                    if (block.getType() == Material.LAVA) {
                        block.setType(Material.OBSIDIAN);
                        changed.add(block.getLocation());
                    }
                }
            }
        }

        changed.removeIf(loc -> {
            if (loc.distance(playerLoc) > 2.5) {
                Block block = loc.getBlock();
                if (block.getType() == Material.OBSIDIAN) {
                    block.setType(Material.LAVA);
                }
                return true;
            }
            return false;
        });
    }
}
