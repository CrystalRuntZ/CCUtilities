package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class UnstripperItem implements CustomItem {

    private static final String LORE_LINE = "§7Unstripper";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private static final Map<Material, Material> REVERT = Map.ofEntries(
            Map.entry(Material.STRIPPED_OAK_LOG, Material.OAK_LOG),
            Map.entry(Material.STRIPPED_SPRUCE_LOG, Material.SPRUCE_LOG),
            Map.entry(Material.STRIPPED_BIRCH_LOG, Material.BIRCH_LOG),
            Map.entry(Material.STRIPPED_JUNGLE_LOG, Material.JUNGLE_LOG),
            Map.entry(Material.STRIPPED_ACACIA_LOG, Material.ACACIA_LOG),
            Map.entry(Material.STRIPPED_DARK_OAK_LOG, Material.DARK_OAK_LOG),
            Map.entry(Material.STRIPPED_MANGROVE_LOG, Material.MANGROVE_LOG),
            Map.entry(Material.STRIPPED_CHERRY_LOG, Material.CHERRY_LOG),
            Map.entry(Material.STRIPPED_BAMBOO_BLOCK, Material.BAMBOO_BLOCK),
            Map.entry(Material.STRIPPED_PALE_OAK_LOG, Material.PALE_OAK_LOG)
    );

    @Override
    public String getIdentifier() {
        return "unstripper";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component c : lore) if (LORE_LINE.equals(serializer.serialize(c))) return true;
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var player = event.getPlayer();
        if (!player.isSneaking()) return;
        var held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material from = block.getType();
        Material to = REVERT.get(from);
        if (to == null) return;

        event.setCancelled(true);

        BlockData oldData = block.getBlockData();
        BlockData newData = to.createBlockData();

        if (oldData instanceof Orientable && newData instanceof Orientable) {
            ((Orientable) newData).setAxis(((Orientable) oldData).getAxis());
        }

        block.setBlockData(newData, true);
    }
}
