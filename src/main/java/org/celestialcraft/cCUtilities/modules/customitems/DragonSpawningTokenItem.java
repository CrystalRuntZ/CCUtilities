package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public class DragonSpawningTokenItem implements CustomItem {

    private static final String IDENTIFIER = "dragon_spawning_token";
    private static final String LORE_LINE = "§7Dragon Spawning Token";
    private static final long COOLDOWN_MILLIS = 30 * 60 * 1000L;

    private static long lastUseTime = 0L;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        long now = System.currentTimeMillis();
        if (now - lastUseTime < COOLDOWN_MILLIS) {
            long remainingMs = COOLDOWN_MILLIS - (now - lastUseTime);
            long mins = (remainingMs / 1000) / 60;
            long secs = (remainingMs / 1000) % 60;
            player.sendMessage(Component.text()
                    .append(Component.text("A dragon was spawned too recently. Try again in ").color(TextColor.color(0xFF5555)))
                    .append(Component.text(mins + "m " + secs + "s").color(TextColor.color(0xFFFFFF)))
                    .append(Component.text(".").color(TextColor.color(0xFF5555))));
            event.setCancelled(true);
            return;
        }

        lastUseTime = now;

        assert item != null;
        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(newAmount);
            player.getInventory().setItemInMainHand(item);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ced spawn random");
            }
        }.runTask(CCUtilities.getInstance());

        player.sendMessage(Component.text("Dragon Spawning Token consumed. Summoning a random dragon...").color(TextColor.color(0x55FF55)));
        event.setCancelled(true);
    }
}
