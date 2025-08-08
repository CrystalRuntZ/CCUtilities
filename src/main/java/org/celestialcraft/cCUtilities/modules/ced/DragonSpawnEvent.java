package org.celestialcraft.cCUtilities.modules.ced;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DragonSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EnderDragon dragon;
    private final DragonType type;

    public DragonSpawnEvent(EnderDragon dragon, DragonType type) {
        this.dragon = dragon;
        this.type = type;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public DragonType getType() {
        return type;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
