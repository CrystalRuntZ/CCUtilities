package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CriticalChestplateItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Critical Chestplate";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "critical_chestplate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
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
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (attacker.isOnGround()) return;
        if (attacker.getFallDistance() <= 0) return;
        if (attacker.isSprinting()) return;
        if (attacker.isInsideVehicle()) return;
        if (attacker.getVelocity().getY() >= 0) return;

        ItemStack chestplate = attacker.getInventory().getChestplate();
        if (chestplate == null || !matches(chestplate)) return;

        double originalDamage = event.getDamage();
        double boostedDamage = originalDamage * 1.2;
        event.setDamage(boostedDamage);

        attacker.getWorld().spawnParticle(
                Particle.CRIT,
                attacker.getLocation().add(0, 1.0, 0),
                10,
                0.3, 0.3, 0.3,
                0.1
        );
    }
}
