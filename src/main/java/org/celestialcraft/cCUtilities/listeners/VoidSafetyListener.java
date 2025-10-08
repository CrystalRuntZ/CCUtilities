package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.celestialcraft.cCUtilities.modules.customenchants.VoidSafetyEnchant;

public class VoidSafetyListener implements Listener {

    private static final String END_WORLD_NAME = "wild_the_end";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (!(event.getEntity() instanceof Player p)) return;

        World w = p.getWorld();
        if (!END_WORLD_NAME.equalsIgnoreCase(w.getName())) return;

        // Only protect if the player is actively holding/wearing an item with the enchant
        if (!VoidSafetyEnchant.isActiveFor(p)) return;

        // Cancel the lethal void damage, then send them to the world's spawn
        event.setCancelled(true);

        Location spawn = w.getSpawnLocation();
        // Safety: reset fall/velocity to avoid chain damage after TP
        p.setFallDistance(0f);
        p.setNoDamageTicks(40); // ~2 seconds of grace

        // Optional: if spawn Y is too low for some reason, nudge up
        if (spawn.getY() < w.getMinHeight() + 5) {
            spawn = new Location(w, spawn.getX(), w.getMinHeight() + 5, spawn.getZ(), spawn.getYaw(), spawn.getPitch());
        }

        p.teleportAsync(spawn).exceptionally(ex -> {
            // Fallback: try the default world spawn if End spawn somehow fails
            World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
            if (fallback != null) {
                Location fb = fallback.getSpawnLocation();
                p.setFallDistance(0f);
                p.setNoDamageTicks(40);
                p.teleportAsync(fb);
            }
            return null;
        });
    }
}
