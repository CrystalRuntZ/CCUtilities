package org.celestialcraft.cCUtilities.modules.quests.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.util.*;

public class WeeklyQuestGenerator {

    private static final Random random = new Random();

    private static final List<QuestType> AVAILABLE_TYPES = List.of(
            QuestType.MINE_BLOCK,
            QuestType.DISCOVER_BIOME,
            QuestType.DAMAGE_MOBS,
            QuestType.GAIN_EXPERIENCE,
            QuestType.KILL_MOBS,
            QuestType.PLACE_BLOCKS,
            QuestType.RUN_DISTANCE,
            QuestType.SWIM_DISTANCE,
            QuestType.ELYTRA_GLIDE,
            QuestType.BREED_ANIMALS,
            QuestType.SMELT_ITEMS,
            QuestType.HARVEST_CROPS
    );

    private static final List<Material> SAFE_BLOCKS = List.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.STONE, Material.OAK_LOG,
            Material.SAND, Material.GRAVEL, Material.COAL_ORE, Material.IRON_ORE
    );

    private static final List<EntityType> SAFE_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER
    );

    public static List<Quest> generateWeeklyQuests(UUID playerId) {
        List<Quest> quests = new ArrayList<>();
        Set<String> usedTypeTargetPairs = new HashSet<>();

        while (quests.size() < 10) {
            QuestType type = AVAILABLE_TYPES.get(random.nextInt(AVAILABLE_TYPES.size()));
            String targetItem = getRandomTargetItem(type);
            String pairKey = type.name() + ":" + (targetItem != null ? targetItem : "none");

            if (usedTypeTargetPairs.contains(pairKey)) continue;

            int target = getRandomTarget(type);

            Quest quest = new Quest(
                    UUID.nameUUIDFromBytes((playerId.toString() + "_weekly_" + quests.size()).getBytes()),
                    "weekly_" + quests.size() + "_" + playerId.toString().substring(0, 8),
                    type,
                    target,
                    0,
                    System.currentTimeMillis(),
                    604800, // 7 days
                    86400,  // 1 day claim window
                    targetItem
            );

            quests.add(quest);
            usedTypeTargetPairs.add(pairKey);
        }

        return quests;
    }

    private static int getRandomTarget(QuestType type) {
        return switch (type) {
            case SWIM_DISTANCE, RUN_DISTANCE, ELYTRA_GLIDE -> 1000 + random.nextInt(9001); // 1000–10000
            case BREED_ANIMALS -> 5 + random.nextInt(96); // 5–100
            default -> 50 + random.nextInt(451); // 50–500
        };
    }

    private static String getRandomTargetItem(QuestType type) {
        return switch (type) {
            case MINE_BLOCK, PLACE_BLOCKS, SMELT_ITEMS, HARVEST_CROPS ->
                    SAFE_BLOCKS.get(random.nextInt(SAFE_BLOCKS.size())).name();
            case KILL_MOBS, BREED_ANIMALS ->
                    SAFE_MOBS.get(random.nextInt(SAFE_MOBS.size())).name();
            default -> null;
        };
    }
}
