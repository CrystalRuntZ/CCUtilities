package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InfiniteEnderPearlItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Set<String> blockedWorlds = Set.of("spawnworld", "shops");
    private final Map<UUID, Boolean> pearlInFlight = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "infinite_ender_pearl";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_PEARL || !item.hasItemMeta() || item.getItemMeta().lore() == null)
            return false;
        for (var line : Objects.requireNonNull(item.getItemMeta().lore())) {
            if ("§7Infinite Ender Pearl".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Get the item from the hand that triggered the event
        EquipmentSlot hand = event.getHand();
        ItemStack item = (hand == EquipmentSlot.HAND) ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!matches(item)) return;

        if (blockedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage("§cYou can't use this item in this world.");
            event.setCancelled(true);
            return;
        }

        UUID uuid = player.getUniqueId();
        if (pearlInFlight.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        pearlInFlight.put(uuid, true);

        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setShooter(player);
        pearl.setItem(item.clone());
    }

    @Override
    public void onTeleport(Player player, PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        pearlInFlight.remove(player.getUniqueId());

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, 100, 0, true, false, false));
    }
}
