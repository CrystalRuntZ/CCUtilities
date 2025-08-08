package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public class RandomMagicalChestplateItem implements CustomItem {

    private static final String RAW_LORE = "&7Random Magical Chestplate";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private final Random random = new Random();

    @Override
    public String getIdentifier() {
        return "random_magical_chestplate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_CHESTPLATE && item.getType() != Material.NETHERITE_CHESTPLATE) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        int roll = 1 + random.nextInt(4); // 1–4
        String command = "si give rmc" + roll + " 1 " + player.getName();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        assert item != null;
        player.getInventory().removeItem(item);
    }
}
