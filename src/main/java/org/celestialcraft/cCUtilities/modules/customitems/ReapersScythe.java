package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ReapersScythe implements CustomItem {

    private static final String RAW_LORE = "§7Reaper's Scythe";
    private static final String LORE_SECT = RAW_LORE.replace('&','§');
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final long WITHER_COOLDOWN_MS = 10_000L;
    private final Map<UUID, Long> lastWitherAppliedToTarget = new HashMap<>();

    private static final Set<EntityType> REAP_TYPES = EnumSet.of(
            EntityType.SKELETON,
            EntityType.ZOMBIE,
            EntityType.ZOMBIFIED_PIGLIN
    );

    @Override
    public String getIdentifier() {
        return "reapers_scythe";
    }

    public String getLoreLine() {
        return RAW_LORE;
    }

    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SWORD"); // adjust if needed
    }

    @Override
    public boolean matches(ItemStack item) {
        return appliesTo(item) && hasLoreLine(item);
    }

    private static boolean hasLoreLine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        for (Component c : lore) {
            if (c == null) continue;
            if (LEGACY.serialize(c).equalsIgnoreCase(LORE_SECT)) return true;
        }
        return false;
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        ItemStack weapon = p.getInventory().getItemInMainHand();
        if (!hasLoreLine(weapon)) return;

        Entity victim = e.getEntity();

        if (victim instanceof Player target) {
            long now = System.currentTimeMillis();
            Long last = lastWitherAppliedToTarget.get(target.getUniqueId());
            if (last == null || (now - last) >= WITHER_COOLDOWN_MS) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 6, 0, true, true, true));
                lastWitherAppliedToTarget.put(target.getUniqueId(), now);
            }
            return;
        }

        EntityType type = victim.getType();
        if (REAP_TYPES.contains(type) && victim instanceof LivingEntity le) {
            e.setDamage(le.getHealth() + 1000.0);
        }
    }

    // Other CustomItem methods can remain empty or implemented as needed
}
