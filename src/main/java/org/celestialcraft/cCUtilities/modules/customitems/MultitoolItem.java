package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MultitoolItem implements CustomItem {

    private static final String LORE_LINE = "§7Multitool";
    private static final List<Material> TOOL_CYCLE = List.of(
            Material.NETHERITE_AXE,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_HOE
    );

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "multitool";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !TOOL_CYCLE.contains(item.getType()) || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        for (Component line : Objects.requireNonNull(meta.lore())) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack current = player.getInventory().getItemInMainHand();
        if (!matches(current)) return;

        Material currentType = current.getType();
        int index = TOOL_CYCLE.indexOf(currentType);
        if (index == -1) return;

        Material nextType = TOOL_CYCLE.get((index + 1) % TOOL_CYCLE.size());

        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        ItemStack newTool = new ItemStack(nextType);
        ItemMeta newMeta = newTool.getItemMeta();
        if (newMeta == null) return;

        newMeta.displayName(meta.displayName());
        newMeta.lore(meta.lore());
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            newMeta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        newTool.setItemMeta(newMeta);
        player.getInventory().setItemInMainHand(newTool);
        event.setCancelled(true);
    }
}
