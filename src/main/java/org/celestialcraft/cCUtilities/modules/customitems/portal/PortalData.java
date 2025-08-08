package org.celestialcraft.cCUtilities.modules.customitems.portal;

import org.bukkit.Location;
import java.util.UUID;

public class PortalData {
    private final UUID gunId;
    private final UUID ownerId;
    private Location left;
    private Location right;

    public PortalData(UUID gunId, UUID ownerId) {
        this.gunId = gunId;
        this.ownerId = ownerId;
    }

    public UUID getGunId() {
        return gunId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Location getLeft() {
        return left;
    }

    public void setLeft(Location left) {
        this.left = left;
    }

    public Location getRight() {
        return right;
    }

    public void setRight(Location right) {
        this.right = right;
    }

    public boolean hasBoth() {
        return left != null && right != null;
    }
}
