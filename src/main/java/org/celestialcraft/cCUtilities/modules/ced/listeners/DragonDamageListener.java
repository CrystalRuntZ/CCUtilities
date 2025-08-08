package org.celestialcraft.cCUtilities.modules.ced.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.celestialcraft.cCUtilities.modules.ced.DamageTracker;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;

public class DragonDamageListener implements Listener {

    private final DragonManager dragonManager;

    public DragonDamageListener(DragonManager dragonManager) {
        this.dragonManager = dragonManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        if (!dragon.equals(dragonManager.getActiveDragon())) return;

        double damage = event.getFinalDamage();
        DamageTracker tracker = dragonManager.getDamageTracker();
        tracker.recordDamage(dragon, player, damage);

        Bukkit.getLogger().info("[CED] " + player.getName() + " dealt " + String.format("%.2f", damage) + " to dragon.");
    }
}
