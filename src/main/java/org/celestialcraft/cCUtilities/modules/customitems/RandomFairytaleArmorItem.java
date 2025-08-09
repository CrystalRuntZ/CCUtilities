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

public class RandomFairytaleArmorItem implements CustomItem {

    private static final String LORE_LINE = "§7Random Fairytale Armor";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "random_fairytale_armor";
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
        int choice = ThreadLocalRandom.current().nextInt(1, 5); // 1..4

        String cmd = switch (choice) {
            case 1 -> "si give fthelm 1 " + player.getName();
            case 2 -> "si give ftchest 1 " + player.getName();
            case 3 -> "si give ftlegs 1 " + player.getName();
            default -> "si give ftboots 1 " + player.getName();
        };

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

        consumeOneMatching(player.getInventory().getContents());
    }

    private void consumeOneMatching(ItemStack[] contents) {
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (matches(stack)) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                } else {
                    contents[i] = null;
                }
                break;
            }
        }
    }
}
