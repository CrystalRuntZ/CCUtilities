package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class LilGrimPetTokenItem implements CustomItem {

    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private static final String LORE_LINE = "§7Lil Grim Pet Token";
    private static final String PERM = "bisectstudios.pet.bs_halloween_lil_grim";
    private static final String COMMAND = "lp user %s permission set bisectstudios.pet.bs_halloween_lil_grim true";

    @Override
    public String getIdentifier() {
        return "lil_grim_pet_token";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> legacy.serialize(line).equalsIgnoreCase(LORE_LINE));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !matches(item)) return;
        Player player = event.getPlayer();
        if (player.hasPermission(PERM)) {
            player.sendActionBar(Component.text("You already own the Lil Grim pet!")
                    .color(TextColor.color(0xFF5555)));
            event.setCancelled(true);
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(COMMAND, player.getName()));
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        player.sendMessage(Component.text("You have unlocked the Lil Grim pet!").color(TextColor.color(0x55FF55)));
        event.setCancelled(true);
    }
}
