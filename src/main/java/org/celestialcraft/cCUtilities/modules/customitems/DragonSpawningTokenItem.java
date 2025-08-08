package org.celestialcraft.cCUtilities.modules.customitems;

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
    private static final long COOLDOWN_MILLIS = 30 * 60 * 1000L; // 30 minutes

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
            long remaining = (COOLDOWN_MILLIS - (now - lastUseTime)) / 1000;
            player.sendMessage("§cA dragon was spawned too recently. Try again in §f" + (remaining / 60) + "m " + (remaining % 60) + "s§c.");
            return;
        }

        lastUseTime = now;

        // Consume 1 token
        assert item != null;
        item.setAmount(item.getAmount() - 1);

        // Run the command
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ced spawn random");
            }
        }.runTask(CCUtilities.getInstance());

        player.sendMessage("§aDragon Spawning Token consumed. Summoning a random dragon...");
    }
}
