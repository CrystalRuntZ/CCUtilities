package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class SlipperyShoesItem implements CustomItem {

    private static final String RAW_LORE = "&7Slippery Shoes";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    @Override
    public String getIdentifier() {
        return "slippery_shoes";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        ItemStack boots = player.getInventory().getBoots();
        if (!matches(boots)) return;

        if (player.isFlying() || player.isGliding() || player.isSwimming() || player.isInsideVehicle()) return;

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        Vector delta = to.clone().subtract(from);
        if (delta.lengthSquared() <= 0.001) return;

        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();
        if (direction.lengthSquared() == 0) return;

        Vector currentVelocity = player.getVelocity();

        // Check if the block directly beneath the player is solid
        Block blockBelow = player.getLocation().subtract(0, 0.1, 0).getBlock();
        boolean isOnGround = blockBelow.getType().isSolid();

        if (isOnGround) {
            Vector slide = direction.multiply(0.15);
            slide.setY(currentVelocity.getY());
            player.setVelocity(slide);
        } else {
            Vector airMomentum = direction.multiply(0.05);
            player.setVelocity(currentVelocity.add(airMomentum));
        }
    }
}
