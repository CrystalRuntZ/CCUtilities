package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FairytaleHatBoxItem implements CustomItem {

    private static final String LORE_LINE = "§7Fairytale Hat Box";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "fairytale_hat_box";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack used = event.getItem();
        if (!matches(used)) return;

        event.setCancelled(true);

        var player = event.getPlayer();
        int choice = ThreadLocalRandom.current().nextInt(1, 9);

        String cmd = switch (choice) {
            case 1 -> "si give wizhat 1 " + player.getName();
            case 2 -> "si give sphat 1 " + player.getName();
            case 3 -> "si give khhat 1 " + player.getName();
            case 4 -> "si give clerichat 1 " + player.getName();
            case 5 -> "si give hchat 1 " + player.getName();
            case 6 -> "si give druidhat 1 " + player.getName();
            case 7 -> "si give jesterhat 1 " + player.getName();
            default -> "si give ohhat 1 " + player.getName();
        };

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

        assert used != null;
        if (used.getAmount() > 1) {
            used.setAmount(used.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
