package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ParticleActiveCache {
    private static final Map<UUID, Boolean> active = new ConcurrentHashMap<>();
    private ParticleActiveCache() {}

    /** Recompute and cache whether player has any matching items (main/off/armor). Returns the new active state. */
    public static boolean update(Player p, Predicate<ItemStack> hasEnchant) {
        if (p == null || hasEnchant == null) return false;

        boolean a = safeTest(hasEnchant, p.getInventory().getItemInMainHand())
                || safeTest(hasEnchant, p.getInventory().getItemInOffHand());

        if (!a) {
            ItemStack[] armor = p.getInventory().getArmorContents();
            for (ItemStack piece : armor) {
                if (safeTest(hasEnchant, piece)) { a = true; break; }
            }
        }

        active.put(p.getUniqueId(), a);
        return a;
    }

    /** Read cached state (false if absent). */
    public static boolean isActive(Player p) {
        if (p == null) return false;
        return Boolean.TRUE.equals(active.get(p.getUniqueId()));
    }

    /** Invalidate a single player. */
    public static void clear(Player p) {
        if (p != null) active.remove(p.getUniqueId());
    }

    /** Invalidate everything (call on plugin disable). */
    public static void clearAll() {
        active.clear();
    }

    // ---- helpers ----
    private static boolean safeTest(Predicate<ItemStack> pred, ItemStack item) {
        try {
            return pred.test(item);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
