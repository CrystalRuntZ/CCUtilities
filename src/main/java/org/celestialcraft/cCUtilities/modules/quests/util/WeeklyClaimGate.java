package org.celestialcraft.cCUtilities.modules.quests.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class WeeklyClaimGate {
    private final NamespacedKey key;
    private final long cooldownMillis;

    public WeeklyClaimGate(Plugin plugin) {
        this(plugin, 7L * 24 * 60 * 60 * 1000); // 7 days
    }

    public WeeklyClaimGate(Plugin plugin, long cooldownMillis) {
        this.key = new NamespacedKey(plugin, "weekly_claim_at");
        this.cooldownMillis = cooldownMillis;
    }

    /** @return true if player can claim now (cooldown satisfied) */
    public boolean canClaim(Player p, long now) {
        Long last = p.getPersistentDataContainer().get(key, PersistentDataType.LONG);
        return last == null || (now - last) >= cooldownMillis;
    }

    /** Call after a successful grant to record the claim time. */
    public void recordClaim(Player p, long now) {
        p.getPersistentDataContainer().set(key, PersistentDataType.LONG, now);
    }

    /** @return millis remaining until next claim (0 if ready) */
    public long millisRemaining(Player p, long now) {
        Long last = p.getPersistentDataContainer().get(key, PersistentDataType.LONG);
        if (last == null) return 0L;
        long rem = cooldownMillis - (now - last);
        return Math.max(0L, rem);
    }

    public static String formatRemaining(long millis) {
        long days = millis / 86_400_000;
        millis %= 86_400_000;
        long hours = millis / 3_600_000;
        millis %= 3_600_000;
        long minutes = millis / 60_000;
        long seconds = (millis % 60_000) / 1000;
        if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}
