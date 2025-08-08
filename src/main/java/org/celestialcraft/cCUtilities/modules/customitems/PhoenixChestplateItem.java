package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public class PhoenixChestplateItem implements CustomItem {

    private static final String LORE_LINE = "§7Phoenix Chestplate";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "phoenix_chestplate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
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
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack chestplate = victim.getInventory().getChestplate();
        if (!matches(chestplate)) return;

        Entity damager = event.getDamager();
        LivingEntity attacker = null;

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof LivingEntity shooter) {
                attacker = shooter;
            }
        } else if (damager instanceof LivingEntity living) {
            attacker = living;
        }

        if (attacker != null && !attacker.equals(victim)) {
            attacker.setFireTicks(100); // 5 seconds of fire
        }
    }
}
