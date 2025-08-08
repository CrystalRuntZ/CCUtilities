package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.List;

public class MapleStrippingAxeItem implements CustomItem {
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "maple_stripping_axe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) return false;
        List<Component> lore = item.getItemMeta().lore();
        assert lore != null;
        for (Component line : lore) {
            if ("§7Maple Stripping Axe".equals(legacy.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (player.getWorld().getName().equalsIgnoreCase("spawnworld")) return;
        if (!ClaimUtils.canBuild(player)) return;

        var block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || !block.getType().name().endsWith("_LOG"))
            return;

        Material stripped = getStrippedLog(block.getType());
        if (stripped == null) return;

        block.setType(stripped);

        var dropLocation = block.getLocation().add(0.5, 1.0, 0.5);
        var honey = new ItemStack(Material.HONEY_BOTTLE);
        player.getWorld().dropItemNaturally(dropLocation, honey).setVelocity(new Vector(0.0, 0.1, 0.0));

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, dropLocation, 20, 0.25, 0.25, 0.25, 0.01);
        player.playSound(block.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, 1f, 1.2f);

        event.setCancelled(true);
    }

    private Material getStrippedLog(Material type) {
        return switch (type) {
            case OAK_LOG -> Material.STRIPPED_OAK_LOG;
            case SPRUCE_LOG -> Material.STRIPPED_SPRUCE_LOG;
            case BIRCH_LOG -> Material.STRIPPED_BIRCH_LOG;
            case JUNGLE_LOG -> Material.STRIPPED_JUNGLE_LOG;
            case ACACIA_LOG -> Material.STRIPPED_ACACIA_LOG;
            case DARK_OAK_LOG -> Material.STRIPPED_DARK_OAK_LOG;
            case MANGROVE_LOG -> Material.STRIPPED_MANGROVE_LOG;
            case CHERRY_LOG -> Material.STRIPPED_CHERRY_LOG;
            case BAMBOO_BLOCK -> Material.STRIPPED_BAMBOO_BLOCK;
            case CRIMSON_STEM -> Material.STRIPPED_CRIMSON_STEM;
            case WARPED_STEM -> Material.STRIPPED_WARPED_STEM;
            case PALE_OAK_LOG -> Material.STRIPPED_PALE_OAK_LOG;
            default -> null;
        };
    }
}
