package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuqinItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "guqin";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (!item.hasItemMeta() || item.getItemMeta().lore() == null) return false;
        List<net.kyori.adventure.text.Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (net.kyori.adventure.text.Component line : lore) {
            if ("§7Gǔqín".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Sound sound = Sound.sound(
                Key.key("custom.guqin.note1"),
                Sound.Source.MASTER,
                1.0f,
                1.0f
        );
        event.getPlayer().playSound(sound);
    }
}
