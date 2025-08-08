package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MoaiItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Set<UUID> walkingPlayers = new HashSet<>();

    @Override
    public String getIdentifier() {
        return "moai";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || item.getItemMeta().lore() == null)
            return false;
        for (Component line : Objects.requireNonNull(item.getItemMeta().lore())) {
            if ("§7Moai".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isValid() || player.isDead()) return;

        if (!hasMoaiItem(player)) {
            restoreSpeed(player);
            walkingPlayers.remove(player.getUniqueId());
            return;
        }

        applyMoaiSpeed(player);

        if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
            if (walkingPlayers.add(player.getUniqueId())) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_STEP, SoundCategory.PLAYERS, 0.6f, 0.8f);
            }
        } else {
            walkingPlayers.remove(player.getUniqueId());
        }
    }

    private boolean hasMoaiItem(Player player) {
        List<ItemStack> items = new ArrayList<>();
        items.add(player.getInventory().getItemInMainHand());
        items.add(player.getInventory().getItemInOffHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) items.add(armor);
        }
        for (ItemStack item : items) {
            if (matches(item)) return true;
        }
        return false;
    }

    private void applyMoaiSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double target = 0.075; // 75% of 0.1
        if (Math.abs(attr.getBaseValue() - target) > 0.001) {
            attr.setBaseValue(target);
        }
    }

    private void restoreSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        if (attr.getBaseValue() < 0.099) {
            attr.setBaseValue(0.1);
        }
    }
}
