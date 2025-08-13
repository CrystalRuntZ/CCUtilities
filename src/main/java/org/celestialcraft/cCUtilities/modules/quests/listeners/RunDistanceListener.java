package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RunDistanceListener implements Listener {

    // buffer meters until we have whole blocks to award
    private final Map<UUID, Double> buffer = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        buffer.put(e.getPlayer().getUniqueId(), 0.0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        buffer.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!ModuleManager.isEnabled("quests")) return;

        Player p = e.getPlayer();
        if (!p.isOnline()) return;
        if (p.isInsideVehicle()) return;
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        if (e.getFrom().getWorld() != e.getTo().getWorld()) return;

        // Exclude other movement quests: glide & swim
        if (p.isGliding()) return;
        if (p.isSwimming() || p.getRemainingAir() < p.getMaximumAir()) return;

        Location from = e.getFrom();
        Location to   = e.getTo();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        // Ignore tiny micro-movements to avoid jitter inflation
        double dist = Math.hypot(dx, dz);
        if (dist < 0.01) return;

        UUID id = p.getUniqueId();
        double acc = buffer.getOrDefault(id, 0.0) + dist;

        // Convert meters to whole blocks
        int wholeBlocks = (int) Math.floor(acc);
        if (wholeBlocks >= 1) {
            // Award progress for RUN_DISTANCE; includes walk, sprint, sprint-jump, falling forward
            QuestProgress.get().addProgress(p, QuestType.RUN_DISTANCE, wholeBlocks);
            acc -= wholeBlocks;
        }
        buffer.put(id, acc);
    }
}
