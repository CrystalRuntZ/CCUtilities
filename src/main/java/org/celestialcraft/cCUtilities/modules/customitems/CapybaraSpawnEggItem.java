package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class CapybaraSpawnEggItem implements CustomItem {

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "capybara_spawn_egg";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta() || item.getItemMeta().lore() == null)
            return false;

        return Objects.requireNonNull(item.getItemMeta().lore()).stream()
                .anyMatch(line -> legacy.serialize(line).equalsIgnoreCase("§7Capybara Spawn Egg"));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !matches(item)) return;

        if (player.hasPermission("pet.capybara")) {
            player.sendMessage(Component.text("⚠ You may only have 1 capybara pet!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + player.getName() + " permission set pet.capybara true");

        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(newAmount);
            player.getInventory().setItemInMainHand(item);
        }

        player.sendMessage(Component.text("You have unlocked your Capybara pet!")
                .color(TextColor.color(0x55FF55)));
    }
}
