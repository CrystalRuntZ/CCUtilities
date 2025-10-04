package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        if (item == null || item.getType() != org.bukkit.Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                serializer.serialize(component).contains(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long elapsed = now - cooldowns.get(uuid);
            if (elapsed < COOLDOWN_MILLIS) {
                long remSeconds = (COOLDOWN_MILLIS - elapsed + 999) / 1000; // round up
                player.sendActionBar(Component.text("⏳ Cooldown: " + remSeconds + "s")
                        .color(TextColor.color(0xFF5555)));
                event.setCancelled(true);
                return;
            }
        }

        Block below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        if (!below.getType().isSolid()) return;

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(1.8).setY(0.5);

        player.setVelocity(velocity);
        cooldowns.put(uuid, now);

        // Particle and sound effects
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);

        event.setCancelled(true);
    }
}
