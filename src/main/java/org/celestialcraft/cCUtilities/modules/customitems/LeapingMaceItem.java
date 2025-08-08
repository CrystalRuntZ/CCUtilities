package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class LeapingMaceItem implements CustomItem {

    private static final String LORE_LINE = "&7Leaping Mace";
    private static final Set<String> DISABLED_WORLDS = Set.of("shops");
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private final Set<UUID> fallImmune = new HashSet<>();

    @Override
    public String getIdentifier() {
        return "leaping_mace";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_AXE || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                serializer.serialize(component).contains(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (DISABLED_WORLDS.contains(player.getWorld().getName().toLowerCase())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        boolean grounded = player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid();
        if (!grounded) return;

        player.setVelocity(new Vector(0, 2.5, 0));
        fallImmune.add(player.getUniqueId());
        event.setCancelled(true);
    }

    @Override
    public void onFallDamage(Player player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (fallImmune.remove(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
