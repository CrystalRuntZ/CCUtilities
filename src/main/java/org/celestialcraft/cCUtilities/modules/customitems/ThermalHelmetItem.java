package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class ThermalHelmetItem implements CustomItem {

    private static final String RAW_LORE = "&7Thermal Helmet";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    @Override
    public String getIdentifier() {
        return "thermal_helmet";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (!matches(helmet)) return;

        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false));
        }

        Material blockType = player.getLocation().getBlock().getType();
        if (blockType == Material.LAVA || blockType == Material.LAVA_CAULDRON) {
            player.setVelocity(player.getVelocity().multiply(1.25));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 2, 0.2, 0.5, 0.2, 0);
        }
    }

    @Override
    public void onFallDamage(Player player, EntityDamageEvent event) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (!matches(helmet)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            event.setCancelled(true);
        }
    }
}
