package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.List;

public class FrostyChestplateItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Frosty Chestplate";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "frosty_chestplate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
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
        Entity victimEntity = event.getEntity();
        if (!(victimEntity instanceof Player victim)) return;

        ItemStack chestplate = victim.getInventory().getChestplate();
        if (chestplate == null || !matches(chestplate)) return;
        if (!victim.getWorld().getName().equalsIgnoreCase("wild")) return;
        if (!ClaimUtils.canBuild(victim, victim.getLocation())) return;

        Entity damager = event.getDamager();
        LivingEntity attacker = null;

        if (damager instanceof LivingEntity le) {
            attacker = le;
        } else if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof LivingEntity le) {
                attacker = le;
            }
        }

        if (attacker != null) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));

            Location loc = victim.getLocation().add(0, 1, 0);
            victim.getWorld().spawnParticle(
                    Particle.ITEM_SNOWBALL,
                    loc,
                    30,
                    0.5, 1.0, 0.5,
                    0.1);
        }
    }
}
