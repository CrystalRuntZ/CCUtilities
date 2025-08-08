package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

public class CripplingEnchant implements CustomEnchant {

    private static final String loreLine = "§7Crippling Effect";
    private static final String metadataKey = "CripplingEffectWeapon";
    private static final int slownessTicks = 100;
    private static final long cooldownMs = 60_000;

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> lastCrippled = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "crippling_effect";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.BOW || item.getType() == Material.CROSSBOW;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (serializer.serialize(line).equals(loreLine)) return true;
        }
        return false;
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (!appliesTo(item) || hasEnchant(item)) return item;
        ItemMeta meta = item.getItemMeta();
        List<Component> existingLore = meta.lore();
        List<Component> lore = (existingLore != null) ? new ArrayList<>(existingLore) : new ArrayList<>();
        lore.add(serializer.deserialize(loreLine));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getLoreLine() {
        return "&7Crippling Effect";
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        ItemStack weapon = null;

        if (event.getDamager() instanceof Player player) {
            attacker = player;
            weapon = attacker.getInventory().getItemInMainHand();
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                attacker = player;
                weapon = getProjectileWeapon(projectile);
            }
        }

        if (attacker == null || weapon == null || !hasEnchant(weapon)) return;

        UUID victimId = victim.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastCrippled.containsKey(victimId) && now - lastCrippled.get(victimId) < cooldownMs) return;

        lastCrippled.put(victimId, now);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessTicks, 4, false, true));
        victim.setSneaking(true);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                victim.setSneaking(false);
            }
        }, slownessTicks);
    }

    @Override
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack bow = event.getBow();
        if (bow != null && hasEnchant(bow)) {
            Projectile projectile = (Projectile) event.getProjectile();
            projectile.setMetadata(metadataKey, new FixedMetadataValue(plugin, bow.clone()));
        }
    }

    private ItemStack getProjectileWeapon(Projectile projectile) {
        if (!projectile.hasMetadata(metadataKey)) return null;
        List<MetadataValue> metadata = projectile.getMetadata(metadataKey);
        for (MetadataValue value : metadata) {
            if (value.getOwningPlugin() == plugin && value.value() instanceof ItemStack item) {
                return item;
            }
        }
        return null;
    }
}
