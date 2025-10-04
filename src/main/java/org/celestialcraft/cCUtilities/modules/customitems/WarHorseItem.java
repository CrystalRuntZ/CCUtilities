package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WarHorseItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "war_horse";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.HORSE_SPAWN_EGG || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        String legacyTag = "§7War Horse";
        for (Component line : lore) {
            if (serializer.serialize(line).equals(legacyTag)) {
                return true;
            }
        }
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        if (!player.getWorld().getName().equalsIgnoreCase("wild")) {
            player.sendMessage(Component.text("You can only summon your War Horse in the wild.").color(NamedTextColor.RED));
            return;
        }

        player.getWorld().spawn(player.getLocation(), Horse.class, horse -> {
            horse.setOwner(player);
            horse.setTamed(true);
            horse.setAdult();

            AttributeInstance healthAttr = horse.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) healthAttr.setBaseValue(33.75);
            horse.setHealth(33.75);

            horse.setJumpStrength(1.0); // capped jump strength
            AttributeInstance speedAttr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(0.3375);

            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        });

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.2f);

        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amount - 1);
        }
    }
}
