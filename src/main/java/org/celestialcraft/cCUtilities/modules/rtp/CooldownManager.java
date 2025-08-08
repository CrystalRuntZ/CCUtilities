package org.celestialcraft.cCUtilities.modules.rtp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public static boolean isOnCooldown(UUID uuid, String type) {
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(type);
        return expiry != null && now < expiry;
    }

    public static void setCooldown(UUID uuid, String type, long seconds) {
        long expiry = System.currentTimeMillis() + (seconds * 1000);
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(type, expiry);
    }

    public static long getTimeLeft(UUID uuid, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(type);
        if (expiry == null) return 0;

        long now = System.currentTimeMillis();
        return Math.max(0, expiry - now); // milliseconds left
    }

    public static String formatTimeLeft(UUID uuid, String type) {
        long millis = getTimeLeft(uuid, type);
        if (millis <= 0) return "0s";

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return remainingSeconds + "s";
        }
    }
}
