package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DragonKillEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EnderDragon dragon;
    private final DragonType type;

    public DragonKillEvent(EnderDragon dragon, DragonType type) {
        this.dragon = dragon;
        this.type = type;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public DragonType getDragonType() {
        return type;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
