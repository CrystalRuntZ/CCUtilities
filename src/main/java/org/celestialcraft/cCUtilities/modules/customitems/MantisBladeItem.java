package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class MantisBladeItem implements CustomItem {

    private static final String LORE_LINE = "&7Mantis Blade";
    private static final long COOLDOWN_MILLIS = 15 * 1000;
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "mantis_blade";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                serializer.serialize(component).contains(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MILLIS) {
            return;
        }

        Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
        if (!below.getType().isSolid()) return;

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(1.8).setY(0.5);

        player.setVelocity(velocity);
        cooldowns.put(uuid, now);
        event.setCancelled(true);
    }
}
