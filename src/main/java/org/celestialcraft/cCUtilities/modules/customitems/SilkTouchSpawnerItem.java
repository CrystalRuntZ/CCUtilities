package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;
import org.celestialcraft.cCUtilities.utils.SpawnerDataUtil;

import java.util.ArrayList;
import java.util.List;

public class SilkTouchSpawnerItem implements CustomItem {

    private static final String RAW_LORE = "&7Silk Touch Spawner Pick";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private static final TextColor LORE_GRAY = NamedTextColor.GRAY;
    private static final TextColor HEX_PURPLE = TextColor.fromHexString("#C1ADFE");

    @Override
    public String getIdentifier() {
        return "silk_touch_spawner_pick";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onBlockBreak(Player player, Block block, ItemStack tool, BlockBreakEvent event) {
        if (!matches(tool)) return;
        if (block.getType() != Material.SPAWNER) return;
        if (!ClaimUtils.canBuild(player, block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType type = spawner.getSpawnedType();

        // Consume the tool
        player.getInventory().removeItem(tool);

        // Create the dropped spawner with metadata
        ItemStack spawnerDrop = new ItemStack(Material.SPAWNER);
        SpawnerDataUtil.setSpawnerType(spawnerDrop, type);

        ItemMeta dropMeta = spawnerDrop.getItemMeta();
        if (dropMeta != null) {
            assert type != null;
            String mobName = capitalize(type.name());
            dropMeta.displayName(Component.text(mobName + " Spawner", NamedTextColor.YELLOW));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Mob Spawner: ", LORE_GRAY).append(Component.text(mobName, HEX_PURPLE)));
            dropMeta.lore(lore);
            spawnerDrop.setItemMeta(dropMeta);
        }

        block.setType(Material.AIR);
        block.getWorld().dropItemNaturally(block.getLocation(), spawnerDrop);
        event.setCancelled(true);
    }

    private String capitalize(String input) {
        String[] parts = input.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }
}
