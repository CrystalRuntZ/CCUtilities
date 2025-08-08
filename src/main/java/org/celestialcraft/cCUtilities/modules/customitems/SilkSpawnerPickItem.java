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

public class SilkSpawnerPickItem implements CustomItem {

    private static final String RAW_LORE = "&7Silk Spawner Pick";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private static final String USES_PREFIX = "Uses Left: ";
    private static final TextColor LORE_GRAY = NamedTextColor.GRAY;
    private static final TextColor HEX_PURPLE = TextColor.fromHexString("#C1ADFE");

    @Override
    public String getIdentifier() {
        return "silk_spawner_pick";
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

        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        List<Component> lore = meta.lore();
        if (lore == null || lore.size() < 2) return;

        Component usesLine = lore.get(1);
        String plain = LegacyComponentSerializer.legacySection().serialize(usesLine);
        if (!plain.startsWith("§7" + USES_PREFIX)) return;

        int usesLeft;
        try {
            String number = plain.substring(("§7" + USES_PREFIX).length());
            usesLeft = Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return;
        }

        usesLeft--;
        if (usesLeft <= 0) {
            player.getInventory().removeItem(tool);
        } else {
            List<Component> newLore = new ArrayList<>(lore);
            newLore.set(1, Component.text(USES_PREFIX + usesLeft, LORE_GRAY));
            meta.lore(newLore);
            tool.setItemMeta(meta);
        }

        ItemStack spawnerDrop = new ItemStack(Material.SPAWNER);
        SpawnerDataUtil.setSpawnerType(spawnerDrop, type);

        ItemMeta dropMeta = spawnerDrop.getItemMeta();
        if (dropMeta != null) {
            assert type != null;
            String mobName = capitalize(type.name());
            dropMeta.displayName(Component.text(mobName + " Spawner", NamedTextColor.YELLOW));
            List<Component> dropLore = new ArrayList<>();
            dropLore.add(Component.text("Mob Spawner: ", LORE_GRAY)
                    .append(Component.text(mobName, HEX_PURPLE)));
            dropMeta.lore(dropLore);
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
