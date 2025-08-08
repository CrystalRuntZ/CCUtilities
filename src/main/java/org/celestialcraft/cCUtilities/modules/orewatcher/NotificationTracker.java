package org.celestialcraft.cCUtilities.modules.orewatcher;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationTracker {
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        Long expireTime = cooldowns.get(uuid);
        return expireTime != null && now < expireTime;
    }

    public void setCooldown(UUID uuid, long durationMillis) {
        cooldowns.put(uuid, System.currentTimeMillis() + durationMillis);
    }
}
