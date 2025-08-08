package org.celestialcraft.cCUtilities.modules.quests.model;

import java.util.Collections;
import java.util.List;

public record QuestTemplate(
        QuestType type,
        String display,
        int target,
        long expiration,
        long claimWindow,
        List<String> rewardCommands,
        String schedule,
        String targetItem
) {
    public QuestTemplate {
        rewardCommands = rewardCommands != null ? rewardCommands : Collections.emptyList();
    }
}
