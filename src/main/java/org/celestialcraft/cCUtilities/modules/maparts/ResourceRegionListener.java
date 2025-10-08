package org.celestialcraft.cCUtilities.modules.maparts;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ResourceRegionListener implements Listener {
    private final ResourceRegionManager manager;

    // anti-spam cooldown for warning messages
    private final Map<UUID, Long> lastWarn = new HashMap<>();
    private static final long WARN_COOLDOWN_MS = 1500;

    public ResourceRegionListener(ResourceRegionManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        var rr = manager.regionAt(e.getBlock().getLocation());
        if (rr == null) return;

        // Require any mapart.claim.<n> or admin to mine
        if (!hasAnyMapartClaimPerm(p)) {
            e.setCancelled(true);
            warnOnce(p, "§cYou need a mapart.claim.<number> permission to mine here.");
            return;
        }

        // Debounced scan; auto-resets when ≥ 70% air
        manager.scanAndMaybeReset(rr);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        var rr = manager.regionAt(e.getBlock().getLocation());
        if (rr == null) return;

        // Block ALL placements in resource regions for non-admins
        if (!isAdmin(p)) {
            e.setCancelled(true);
            warnOnce(p, "§cYou cannot place blocks in this resource region.");
        }
    }

    private void warnOnce(Player p, String msg) {
        long now = System.currentTimeMillis();
        long last = lastWarn.getOrDefault(p.getUniqueId(), 0L);
        if (now - last >= WARN_COOLDOWN_MS) {
            p.sendMessage(msg);
            lastWarn.put(p.getUniqueId(), now);
        }
    }

    /** Matches any mapart.claim.<number> or admin perms. */
    private boolean hasAnyMapartClaimPerm(Player p) {
        if (isAdmin(p)) return true;
        return p.getEffectivePermissions().stream()
                .map(ep -> ep.getPermission().toLowerCase(Locale.ROOT))
                .anyMatch(perm -> {
                    if (!perm.startsWith("mapart.claim.")) return false;
                    String[] parts = perm.split("\\.");
                    if (parts.length != 3) return false;
                    try {
                        Integer.parseInt(parts[2]); // ensure it's mapart.claim.<int>
                        return p.hasPermission("mapart.claim." + parts[2]);
                    } catch (NumberFormatException ignore) {
                        return false;
                    }
                });
    }

    private boolean isAdmin(Player p) {
        return p.hasPermission("maparts.admin") || p.hasPermission("mapart.admin");
    }
}
