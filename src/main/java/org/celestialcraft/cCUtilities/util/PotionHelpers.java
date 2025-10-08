package org.celestialcraft.cCUtilities.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PotionHelpers {
    private PotionHelpers() {}

    /**
     * Add or refresh a potion effect only if it's stronger or meaningfully extends duration.
     * @return true if we changed the entity's effects
     */
    public static boolean addOrRefresh(LivingEntity le,
                                       PotionEffectType type,
                                       int amplifier,
                                       int durationTicks,
                                       boolean ambient,
                                       boolean particles,
                                       boolean icon) {
        if (le == null || type == null || durationTicks <= 0) return false;

        PotionEffect current = le.getPotionEffect(type);
        if (current == null) {
            return le.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon));
        }

        int curAmp = current.getAmplifier();
        int curDur = current.getDuration();

        // If current is stronger, keep it
        if (curAmp > amplifier) return false;

        // If equal strength, only refresh if we'd gain a meaningful amount (~10 ticks slack)
        if (curAmp == amplifier && curDur >= durationTicks - 10) return false;

        return le.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon), true);
    }

    /** Convenience: hidden, non-ambient status effect (good for “perma” buffs). */
    public static boolean addHiddenOrRefresh(LivingEntity le, PotionEffectType type, int amplifier, int durationTicks) {
        return addOrRefresh(le, type, amplifier, durationTicks, true, false, false);
    }

    /** Remove if present. */
    public static boolean removeIfPresent(LivingEntity le, PotionEffectType type) {
        if (le == null || type == null) return false;
        if (!le.hasPotionEffect(type)) return false;
        le.removePotionEffect(type);
        return true;
    }
}
