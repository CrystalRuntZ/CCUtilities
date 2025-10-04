package org.celestialcraft.cCUtilities.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ClaimUtils {

    public static boolean canBuild(Player player) {
        return canBuild(player, player.getLocation());
    }

    public static boolean canBuild(Player player, Location location) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (plugin == null || !plugin.isEnabled()) return true;

        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object instance = gpClass.getDeclaredField("instance").get(null);
            Object dataStore = gpClass.getMethod("getDataStore").invoke(instance);

            Object claim = dataStore.getClass()
                    .getMethod("getClaimAt", Location.class, boolean.class, Object.class)
                    .invoke(dataStore, location, false, null);
            if (claim == null) return true;

            Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            boolean hasPermission = (boolean) claimClass
                    .getMethod("hasExplicitPermission", Player.class, String.class)
                    .invoke(claim, player, "Build");

            Object ownerId = claimClass.getField("ownerID").get(claim);
            return hasPermission || ownerId.equals(player.getUniqueId());
        } catch (Exception e) {
            return true;
        }
    }

    // ---- NEW: Owner-only checks (don’t allow wilderness or just-trusted) ----

    /** True only if there is a claim at location AND the player is that claim's owner. */
    public static boolean isOwner(Player player, Location location) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (plugin == null || !plugin.isEnabled()) return true; // if GP missing, don't hard-block usage

        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object instance = gpClass.getDeclaredField("instance").get(null);
            Object dataStore = gpClass.getMethod("getDataStore").invoke(instance);

            Object claim = dataStore.getClass()
                    .getMethod("getClaimAt", Location.class, boolean.class, Object.class)
                    .invoke(dataStore, location, false, null);
            if (claim == null) return false; // wilderness is NOT "own claim"

            Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            Object ownerId = claimClass.getField("ownerID").get(claim);
            return ownerId != null && ownerId.equals(player.getUniqueId());
        } catch (Exception e) {
            return false; // on error, be safe and deny "owner" check
        }
    }

    public static boolean isOwnerOfChunk(Player player, Chunk chunk) {
        Location playerLoc = player.getLocation();
        Location locToCheck = new Location(
                chunk.getWorld(),
                (chunk.getX() << 4) + 8,
                playerLoc.getY(),
                (chunk.getZ() << 4) + 8
        );
        return isOwner(player, locToCheck);
    }
}
