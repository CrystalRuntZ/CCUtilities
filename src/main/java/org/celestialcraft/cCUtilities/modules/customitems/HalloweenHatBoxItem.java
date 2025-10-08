package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HalloweenHatBoxItem implements CustomItem {

    private static final String LORE_LINE = "§7Fall Hat Box";

    @Override
    public String getIdentifier() {
        return "fall_hat_box";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        String targetText = LegacyComponentSerializer.legacySection().serialize(Component.text(LORE_LINE)).trim().toLowerCase();

        for (Component line : lore) {
            if (line == null) continue;
            String lineText = LegacyComponentSerializer.legacySection().serialize(line).trim().toLowerCase();
            if (lineText.equals(targetText)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        String playerName = player.getName();

        int number = ThreadLocalRandom.current().nextInt(1, 11); // 1-10 inclusive

        String command = String.format("si give fh%d 1 %s", number, playerName);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage("§aYou have opened a Halloween Hat Box!");

        // Remove the item from player's hand (consume the box)
        assert item != null;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        event.setCancelled(true);
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        // No behavior for non-sneak right click
    }
}
