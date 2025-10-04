package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CondenseCommandItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Condense Command";
    private static final String PERMISSION_NODE = "essentials.condense";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "condense_command";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;
        if (!player.isSneaking()) return;

        if (player.hasPermission(PERMISSION_NODE)) {
            player.sendMessage(Component.text("⚠ You already have access to the condense command!")
                    .color(TextColor.color(0xFF5555)));
            event.setCancelled(true);
            return;
        }

        String command = "lp user " + player.getName() + " permission set " + PERMISSION_NODE + " true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Decrease the held item amount by 1 or remove if last
        assert item != null;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(Component.text("You have been granted access to the condense command!")
                .color(TextColor.color(0x55FF55)));

        event.setCancelled(true);
    }
}
