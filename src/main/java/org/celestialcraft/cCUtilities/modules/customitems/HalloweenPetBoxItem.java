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
import java.util.concurrent.ThreadLocalRandom;

public class HalloweenPetBoxItem implements CustomItem {

    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private static final String LORE_LINE = "§7Halloween Pet Box";

    private static final String[] ITEM_COMMANDS = {
            "si give hwgrim 1 %s",
            "si give hwjack 1 %s",
            "si give hwbroom 1 %s"
    };

    @Override
    public String getIdentifier() {
        return "halloween_pet_box";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> legacy.serialize(line).equalsIgnoreCase(LORE_LINE));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        ItemStack item = event.getItem();
        if (item == null || !matches(item)) return;
        Player player = event.getPlayer();

        int chosen = ThreadLocalRandom.current().nextInt(ITEM_COMMANDS.length);
        String giveCmd = String.format(ITEM_COMMANDS[chosen], player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);

        player.sendMessage(Component.text("You have received a special Halloween pet item!").color(TextColor.color(0x55FF55)));

        // Remove the item from main hand (consume the box)
        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(newAmount);
            player.getInventory().setItemInMainHand(item);
        }

        event.setCancelled(true);
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        // No action for regular right click
    }
}
