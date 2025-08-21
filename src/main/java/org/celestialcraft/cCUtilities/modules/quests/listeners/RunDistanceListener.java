package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
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

    // carry over sub-1.0 block movement so sneaking/bobbing steps still count
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

        final Player p = e.getPlayer();
        final Location from = e.getFrom();
        final Location to = e.getTo();

        // world changes (teleports/portals) -> ignore this tick
        if (from.getWorld() != to.getWorld()) return;

        // exclude non-running contexts
        if (!p.isOnline()) return;
        if (p.isInsideVehicle() && !(p.getVehicle() instanceof Boat)) return; // any vehicle -> ignore
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        // Creative flight (or any flight) shouldn't count as running
        if (p.isFlying()) return;
        // other movement quests take precedence
        if (p.isGliding()) return;
        // swimming / underwater (breath meter going down) -> handled by Swim quest
        if (p.isSwimming() || p.getRemainingAir() < p.getMaximumAir()) return;

        // horizontal distance only (so pure vertical changes don't inflate running)
        final double dx = to.getX() - from.getX();
        final double dz = to.getZ() - from.getZ();
        final double dist = Math.hypot(dx, dz);

        // ignore micro jitter
        if (dist < 0.01) return;

        final UUID id = p.getUniqueId();
        double acc = buffer.getOrDefault(id, 0.0) + dist;

        // award whole blocks; keep the remainder
        final int whole = (int) Math.floor(acc);
        if (whole > 0) {
            QuestProgress.get().addProgress(p, QuestType.RUN_DISTANCE, whole);
            acc -= whole;
        }
        buffer.put(id, acc);
    }
}
