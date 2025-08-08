package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class DamageTracker {

    final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();
    private final Map<UUID, Double> virtualMaxHealth = new HashMap<>();

    public void register(LivingEntity entity, double maxHealth) {
        UUID id = entity.getUniqueId();
        damageMap.put(id, new HashMap<>());
        virtualMaxHealth.put(id, maxHealth);
    }

    public void recordDamage(Entity target, Player damager, double amount) {
        if (!(target instanceof LivingEntity)) return;
        UUID id = target.getUniqueId();
        if (!damageMap.containsKey(id)) return;
        damageMap.get(id).merge(damager.getUniqueId(), amount, Double::sum);
    }

    public List<Map.Entry<UUID, Double>> getTopDamagers(Entity target, int count) {
        return getTopDamagers(target.getUniqueId(), count);
    }

    public List<Map.Entry<UUID, Double>> getTopDamagers(UUID entityId, int count) {
        Map<UUID, Double> map = damageMap.get(entityId);
        if (map == null) return Collections.emptyList();
        return map.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(count)
                .toList();
    }

    public double getProgress(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        Double max = virtualMaxHealth.get(id);
        if (max == null || max <= 0) return 1.0;
        double remaining = Math.max(0, entity.getHealth());
        return remaining / max;
    }

    public void clear(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        damageMap.remove(id);
        virtualMaxHealth.remove(id);
    }

    public double getDamage(UUID entityId, UUID playerId) {
        Map<UUID, Double> map = damageMap.get(entityId);
        if (map == null) return 0.0;
        return map.getOrDefault(playerId, 0.0);
    }

    public Map<UUID, Double> getAllDamagers(UUID entityId) {
        return damageMap.getOrDefault(entityId, new HashMap<>());
    }
}
