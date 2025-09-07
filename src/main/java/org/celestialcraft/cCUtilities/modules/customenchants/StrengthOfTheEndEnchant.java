package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.projectiles.ProjectileSource;
import org.celestialcraft.cCUtilities.util.LoreUtil;

public class StrengthOfTheEndEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Strength of the End";
    private static final double MULTIPLIER = 1.5D;

    @Override public String getIdentifier() { return "strength_of_the_end"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && isWeapon(item.getType());
    }

    private boolean isWeapon(Material m) {
        return switch (m) {
            // melee
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 WOODEN_AXE,   STONE_AXE,   IRON_AXE,   GOLDEN_AXE,   DIAMOND_AXE,   NETHERITE_AXE,
                 MACE,
                 // ranged
                 BOW, CROSSBOW,
                 // thrown
                 TRIDENT -> true;
            default -> false;
        };
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
        if (event.isCancelled()) return;

        Entity damager = event.getDamager();
        boolean enchantedAttack = false;

        // 1) Melee attackers with weapon in hand(s)
        if (damager instanceof LivingEntity le) {
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                enchantedAttack = hasEnchant(eq.getItemInMainHand()) || hasEnchant(eq.getItemInOffHand());
            }
        }

        // 2) Projectiles (bow/crossbow and thrown tridents)
        if (!enchantedAttack && damager instanceof Projectile proj) {
            if (damager instanceof Trident tri) {
                // non-deprecated
                enchantedAttack = hasEnchant(tri.getItemStack());
            }
            if (!enchantedAttack) {
                ProjectileSource src = proj.getShooter();
                if (src instanceof LivingEntity shooter) {
                    EntityEquipment eq = shooter.getEquipment();
                    if (eq != null) {
                        enchantedAttack = hasEnchant(eq.getItemInMainHand()) || hasEnchant(eq.getItemInOffHand());
                    }
                }
            }
        }

        if (!enchantedAttack) return;

        Entity target = event.getEntity();
        if (isEndAligned(target)) {
            event.setDamage(event.getDamage() * MULTIPLIER);
        }
    }

    private boolean isEndAligned(Entity e) {
        if (e instanceof EnderDragon || e instanceof EnderDragonPart) return true;
        return (e instanceof Enderman) || (e instanceof Endermite) || (e instanceof Shulker);
    }

    // Unused hooks for your interface
    @Override public void onHeld(PlayerItemHeldEvent event) { /* no-op */ }
    @Override public void onHandSwap(Player player)         { /* no-op */ }
    @Override public void onPlayerMove(Player player)       { /* no-op */ }
    @Override public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) { /* no-op */ }
    @Override public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { /* no-op */ }
}
