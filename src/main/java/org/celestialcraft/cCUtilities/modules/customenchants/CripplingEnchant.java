package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.*;

public class CripplingEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Crippling Effect";
    private static final String METADATA_KEY = "CripplingEffectWeapon";
    private static final int SLOWNESS_TICKS = 100;   // 5s
    private static final long COOLDOWN_MS   = 60_000;

    private final Map<UUID, Long> lastCrippled = new HashMap<>();

    @Override public String getIdentifier() { return "crippling_effect"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        Material t = item.getType();
        return t == Material.BOW || t == Material.CROSSBOW;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        ItemStack weapon = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
            weapon = p.getInventory().getItemInMainHand();
        } else if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) {
                attacker = p;
                weapon = getProjectileWeapon(proj);
                if (weapon == null) weapon = p.getInventory().getItemInMainHand();
            }
        }

        if (attacker == null || !hasEnchant(weapon)) return;

        UUID vid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastCrippled.get(vid);
        if (last != null && (now - last) < COOLDOWN_MS) return;

        lastCrippled.put(vid, now);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_TICKS, 4, false, true, true));
        victim.setSneaking(true);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3);

        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> {
            if (victim.isOnline()) victim.setSneaking(false);
        }, SLOWNESS_TICKS);
    }

    @Override
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack bow = event.getBow();
        if (bow != null && hasEnchant(bow) && event.getProjectile() instanceof Projectile proj) {
            proj.setMetadata(METADATA_KEY, new FixedMetadataValue(CCUtilities.getInstance(), bow.clone()));
        }
    }

    private ItemStack getProjectileWeapon(Projectile projectile) {
        if (!projectile.hasMetadata(METADATA_KEY)) return null;
        for (MetadataValue v : projectile.getMetadata(METADATA_KEY)) {
            if (v.getOwningPlugin() == CCUtilities.getInstance() && v.value() instanceof ItemStack is) {
                return is;
            }
        }
        return null;
    }
}
