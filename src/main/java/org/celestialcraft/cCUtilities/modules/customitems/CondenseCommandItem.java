package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
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

    private static final String LORE_IDENTIFIER = "&7Condense Command";
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

        String formatted = LORE_IDENTIFIER.replace("&", "§");
        for (Component line : lore) {
            if (legacy.serialize(line).equalsIgnoreCase(formatted)) {
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

        // If player already has permission, block usage
        if (player.hasPermission(PERMISSION_NODE)) {
            player.sendMessage("§c⚠ You already have access to the condense command!");
            event.setCancelled(true);
            return;
        }

        // Run the command as console to grant permission
        String command = "lp user " + player.getName() + " permission set essentials.condense true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Remove the item from the player's inventory
        assert item != null;
        player.getInventory().removeItem(item);

        event.setCancelled(true);
    }
}
