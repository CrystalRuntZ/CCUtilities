package org.celestialcraft.cCUtilities.modules.customitems;

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

        // Check if player already has the permission
        if (player.hasPermission("pet.capybara")) {
            player.sendMessage("§c⚠ You may only have 1 capybara pet!");
            return;
        }

        // Grant permission via console command (works with any perm plugin)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + player.getName() + " permission set pet.capybara true");

        // Consume exactly one item
        item.setAmount(item.getAmount() - 1);

        player.sendMessage("§aYou have unlocked your Capybara pet!");
    }
}
