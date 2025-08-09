package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WaxyEffectItem implements CustomItem {

    private static final String LORE_LINE = "§7Waxy Effect";
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final NamespacedKey WAXY_STAGE_KEY = new NamespacedKey(CCUtilities.getInstance(), "waxy_stage");

    private enum Stage {
        NORMAL(0, "Normal"),
        EXPOSED(1, "Exposed"),
        WEATHERED(2, "Weathered"),
        OXIDIZED(3, "Oxidized");

        final int id;
        final String label;

        Stage(int id, String label) { this.id = id; this.label = label; }

        static Stage fromId(int id) {
            return switch (id) {
                case 1 -> EXPOSED;
                case 2 -> WEATHERED;
                case 3 -> OXIDIZED;
                default -> NORMAL;
            };
        }

        Stage next() {
            return fromId((this.id + 1) % 4);
        }
    }

    private static final Map<Material, Material> WAX_MAP = new LinkedHashMap<>();
    static {
        WAX_MAP.put(Material.COPPER_BLOCK, Material.WAXED_COPPER_BLOCK);
        WAX_MAP.put(Material.EXPOSED_COPPER, Material.WAXED_EXPOSED_COPPER);
        WAX_MAP.put(Material.WEATHERED_COPPER, Material.WAXED_WEATHERED_COPPER);
        WAX_MAP.put(Material.OXIDIZED_COPPER, Material.WAXED_OXIDIZED_COPPER);
        WAX_MAP.put(Material.CUT_COPPER, Material.WAXED_CUT_COPPER);
        WAX_MAP.put(Material.EXPOSED_CUT_COPPER, Material.WAXED_EXPOSED_CUT_COPPER);
        WAX_MAP.put(Material.WEATHERED_CUT_COPPER, Material.WAXED_WEATHERED_COPPER);
        WAX_MAP.put(Material.OXIDIZED_CUT_COPPER, Material.WAXED_OXIDIZED_COPPER);
        WAX_MAP.put(Material.CUT_COPPER_STAIRS, Material.WAXED_CUT_COPPER_STAIRS);
        WAX_MAP.put(Material.EXPOSED_CUT_COPPER_STAIRS, Material.WAXED_EXPOSED_CUT_COPPER_STAIRS);
        WAX_MAP.put(Material.WEATHERED_CUT_COPPER_STAIRS, Material.WAXED_WEATHERED_CUT_COPPER_STAIRS);
        WAX_MAP.put(Material.OXIDIZED_CUT_COPPER_STAIRS, Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        WAX_MAP.put(Material.CUT_COPPER_SLAB, Material.WAXED_CUT_COPPER_SLAB);
        WAX_MAP.put(Material.EXPOSED_CUT_COPPER_SLAB, Material.WAXED_EXPOSED_CUT_COPPER_SLAB);
        WAX_MAP.put(Material.WEATHERED_CUT_COPPER_SLAB, Material.WAXED_WEATHERED_CUT_COPPER_SLAB);
        WAX_MAP.put(Material.OXIDIZED_CUT_COPPER_SLAB, Material.WAXED_OXIDIZED_CUT_COPPER_SLAB);
        WAX_MAP.put(Material.CHISELED_COPPER, Material.WAXED_CHISELED_COPPER);
        WAX_MAP.put(Material.EXPOSED_CHISELED_COPPER, Material.WAXED_EXPOSED_CHISELED_COPPER);
        WAX_MAP.put(Material.WEATHERED_CHISELED_COPPER, Material.WAXED_WEATHERED_COPPER);
        WAX_MAP.put(Material.OXIDIZED_CHISELED_COPPER, Material.WAXED_OXIDIZED_COPPER);
        WAX_MAP.put(Material.COPPER_BULB, Material.WAXED_COPPER_BULB);
        WAX_MAP.put(Material.EXPOSED_COPPER_BULB, Material.WAXED_EXPOSED_COPPER_BULB);
        WAX_MAP.put(Material.WEATHERED_COPPER_BULB, Material.WAXED_WEATHERED_COPPER_BULB);
        WAX_MAP.put(Material.OXIDIZED_COPPER_BULB, Material.WAXED_OXIDIZED_COPPER_BULB);
        WAX_MAP.put(Material.COPPER_GRATE, Material.WAXED_COPPER_GRATE);
        WAX_MAP.put(Material.EXPOSED_COPPER_GRATE, Material.WAXED_EXPOSED_COPPER_GRATE);
        WAX_MAP.put(Material.WEATHERED_COPPER_GRATE, Material.WAXED_WEATHERED_COPPER_GRATE);
        WAX_MAP.put(Material.OXIDIZED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE);
        WAX_MAP.put(Material.COPPER_DOOR, Material.WAXED_COPPER_DOOR);
        WAX_MAP.put(Material.EXPOSED_COPPER_DOOR, Material.WAXED_EXPOSED_COPPER_DOOR);
        WAX_MAP.put(Material.WEATHERED_COPPER_DOOR, Material.WAXED_WEATHERED_COPPER_DOOR);
        WAX_MAP.put(Material.OXIDIZED_COPPER_DOOR, Material.WAXED_OXIDIZED_COPPER_DOOR);
        WAX_MAP.put(Material.COPPER_TRAPDOOR, Material.WAXED_COPPER_TRAPDOOR);
        WAX_MAP.put(Material.EXPOSED_COPPER_TRAPDOOR, Material.WAXED_EXPOSED_COPPER_TRAPDOOR);
        WAX_MAP.put(Material.WEATHERED_COPPER_TRAPDOOR, Material.WAXED_WEATHERED_COPPER_TRAPDOOR);
        WAX_MAP.put(Material.OXIDIZED_COPPER_TRAPDOOR, Material.WAXED_OXIDIZED_COPPER_TRAPDOOR);
    }

    @Override
    public String getIdentifier() {
        return "waxy_effect";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component c : lore) {
            if (LORE_LINE.equals(SERIALIZER.serialize(c))) return true;
        }
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        var player = event.getPlayer();
        var held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        var action = event.getAction();

        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && !player.isSneaking()) {
            int current = player.getPersistentDataContainer().getOrDefault(WAXY_STAGE_KEY, PersistentDataType.INTEGER, 0);
            Stage next = Stage.fromId(current).next();
            player.getPersistentDataContainer().set(WAXY_STAGE_KEY, PersistentDataType.INTEGER, next.id);
            player.sendMessage(Component.text("Selected: ").color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(next.label, NamedTextColor.WHITE)));
            return;
        }

        if ((action == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            Material to = WAX_MAP.get(block.getType());
            if (to == null) return;
            event.setCancelled(true);
            replaceKeepingCommonState(block, to);
            return;
        }

        if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) && player.isSneaking()) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Material type = block.getType();
            if (!isCopperBlockOrWaxed(type)) return;

            int stageId = player.getPersistentDataContainer().getOrDefault(WAXY_STAGE_KEY, PersistentDataType.INTEGER, 0);
            Stage stage = Stage.fromId(stageId);

            Material target = switch (stage) {
                case NORMAL -> Material.COPPER_BLOCK;
                case EXPOSED -> Material.EXPOSED_COPPER;
                case WEATHERED -> Material.WEATHERED_COPPER;
                case OXIDIZED -> Material.OXIDIZED_COPPER;
            };

            event.setCancelled(true);
            replaceKeepingCommonState(block, target);
        }
    }

    private static boolean isCopperBlockOrWaxed(Material m) {
        return m == Material.COPPER_BLOCK ||
                m == Material.EXPOSED_COPPER ||
                m == Material.WEATHERED_COPPER ||
                m == Material.OXIDIZED_COPPER ||
                m == Material.WAXED_COPPER_BLOCK ||
                m == Material.WAXED_EXPOSED_COPPER ||
                m == Material.WAXED_WEATHERED_COPPER ||
                m == Material.WAXED_OXIDIZED_COPPER;
    }

    private void replaceKeepingCommonState(Block block, Material to) {
        BlockData oldData = block.getBlockData();
        BlockData newData = to.createBlockData();

        if (oldData instanceof Orientable o && newData instanceof Orientable n) n.setAxis(o.getAxis());
        if (oldData instanceof Directional o && newData instanceof Directional n) n.setFacing(o.getFacing());
        if (oldData instanceof Stairs o && newData instanceof Stairs n) {
            n.setFacing(o.getFacing());
            n.setShape(o.getShape());
            n.setHalf(o.getHalf());
            n.setWaterlogged(o.isWaterlogged());
        }
        if (oldData instanceof Slab o && newData instanceof Slab n) {
            n.setType(o.getType());
            n.setWaterlogged(o.isWaterlogged());
        }
        if (oldData instanceof Openable o && newData instanceof Openable n) n.setOpen(o.isOpen());
        if (oldData instanceof Powerable o && newData instanceof Powerable n) n.setPowered(o.isPowered());
        if (oldData instanceof Bisected o && newData instanceof Bisected n) n.setHalf(o.getHalf());
        if (oldData instanceof Waterlogged o && newData instanceof Waterlogged n) n.setWaterlogged(o.isWaterlogged());

        block.setBlockData(newData, true);
    }
}
