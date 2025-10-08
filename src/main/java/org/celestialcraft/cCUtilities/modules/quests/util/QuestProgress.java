package org.celestialcraft.cCUtilities.modules.quests.util;

import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.UUID;

public final class QuestProgress {
    private static QuestProgressService svc;
    private QuestProgress() {}

    public static QuestProgressService get() {
        if (svc == null) svc = new QuestProgressService(CCUtilities.getInstance());
        return svc;
    }

    // Convenience helpers for admin flows
    public static int removeInvalidWeeklyItems(Player p) {
        return get().removeInvalidWeeklyItems(p);
    }

    public static int purgeOwner(UUID owner) {
        return get().purgeOwner(owner);
    }
}
