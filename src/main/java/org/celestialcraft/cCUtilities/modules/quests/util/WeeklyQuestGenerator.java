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

    // Blocks suitable for mining/placing quests
    private static final List<Material> SAFE_MINE_BLOCKS = List.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE,
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE
    );
    private static final List<Material> SAFE_PLACE_BLOCKS = List.of(
            Material.OAK_PLANKS, Material.COBBLESTONE, Material.STONE_BRICKS,
            Material.TORCH, Material.GLASS, Material.OAK_LOG
    );

    // 🔒 Hard blacklist for place/break targets
    private static final Set<Material> BLOCK_BLACKLIST = EnumSet.of(
            Material.BEDROCK,
            Material.OBSIDIAN,
            Material.REINFORCED_DEEPSLATE,
            Material.END_PORTAL_FRAME,
            Material.BARRIER,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK
    );

    // Harvestable crops (Ageable in your listener)
    private static final List<Material> SAFE_CROPS = List.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART, Material.COCOA,
            Material.SWEET_BERRY_BUSH
    );

    // Smelt result items
    private static final List<Material> SAFE_SMELT_RESULTS = List.of(
            Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT,
            Material.GLASS, Material.STONE,
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON, Material.COOKED_CHICKEN
    );

    // Non-boss, common hostile mobs
    private static final List<EntityType> SAFE_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
            EntityType.ENDERMAN, EntityType.SLIME, EntityType.DROWNED, EntityType.HUSK
    );

    // Animals for breeding
    private static final List<EntityType> SAFE_BREED = List.of(
            EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
            EntityType.RABBIT, EntityType.WOLF, EntityType.CAT, EntityType.HORSE
    );

    // Biomes to target (use the vanilla key "FLOWER_FOREST", "SAVANNA", etc.)
    private static final List<String> SAFE_BIOMES = List.of(
            "FLOWER_FOREST", "MEADOW", "SAVANNA", "CHERRY_GROVE",
            "JUNGLE", "DESERT", "MUSHROOM_FIELDS", "BADLANDS",
            "WINDSWEPT_HILLS", "STONY_PEAKS"
    );

    public static List<Quest> generateWeeklyQuests(UUID playerId) {
        List<Quest> quests = new ArrayList<>();
        Set<String> usedTypeTargetPairs = new HashSet<>();

        while (quests.size() < 10) {
            QuestType type = AVAILABLE_TYPES.get(random.nextInt(AVAILABLE_TYPES.size()));
            String targetItem = getRandomTargetItem(type);
            String pairKey = type.name() + ":" + (targetItem != null ? targetItem : "none");
            if (usedTypeTargetPairs.contains(pairKey)) continue;

            int target = getTargetAmount(type);

            Quest quest = new Quest(
                    UUID.nameUUIDFromBytes((playerId.toString() + "_weekly_" + quests.size()).getBytes()),
                    "weekly_" + quests.size() + "_" + playerId.toString().substring(0, 8),
                    type,
                    target,
                    0,
                    System.currentTimeMillis(),
                    604800, // 7 days (seconds)
                    86400,  // 1 day claim window (seconds)
                    targetItem
            );

            quests.add(quest);
            usedTypeTargetPairs.add(pairKey);
        }

        return quests;
    }

    private static int getTargetAmount(QuestType type) {
        // Make biomes always single-target ✅
        if (type == QuestType.DISCOVER_BIOME) return 1;

        return switch (type) {
            case RUN_DISTANCE, SWIM_DISTANCE -> 1000 + random.nextInt(9001);   // 1000–10000
            case ELYTRA_GLIDE -> 500 + random.nextInt(4501);                   // 500–5000
            case DAMAGE_MOBS -> 500 + random.nextInt(4501);                    // 500–5000 damage
            case GAIN_EXPERIENCE -> 250 + random.nextInt(2251);                // 250–2500 xp
            case KILL_MOBS -> 25 + random.nextInt(126);                        // 25–150 kills
            case BREED_ANIMALS -> 5 + random.nextInt(26);                      // 5–30 breeds
            case MINE_BLOCK, PLACE_BLOCKS -> 64 + random.nextInt(193);         // 64–256 blocks
            case SMELT_ITEMS -> 32 + random.nextInt(97);                       // 32–128 items
            case HARVEST_CROPS -> 32 + random.nextInt(97);                     // 32–128 crops
            default -> 50 + random.nextInt(451);
        };
    }

    private static String getRandomTargetItem(QuestType type) {
        return switch (type) {
            case DISCOVER_BIOME -> pickBiome();
            case KILL_MOBS -> pickEntityName(SAFE_MOBS);          // bosses excluded elsewhere too
            case BREED_ANIMALS -> pickEntityName(SAFE_BREED);
            case MINE_BLOCK -> pickMatNameFiltered(SAFE_MINE_BLOCKS);  // 🔒 blacklist applied
            case PLACE_BLOCKS -> pickMatNameFiltered(SAFE_PLACE_BLOCKS); // 🔒 blacklist applied
            case HARVEST_CROPS -> pickMatName(SAFE_CROPS);
            case SMELT_ITEMS -> pickMatName(SAFE_SMELT_RESULTS);
            default -> null; // distance/xp/damage quests are non-targeted
        };
    }

    private static String pickMatName(List<Material> mats) {
        return mats.get(random.nextInt(mats.size())).name();
    }

    private static String pickMatNameFiltered(List<Material> mats) {
        List<Material> pool = mats.stream().filter(m -> !WeeklyQuestGenerator.BLOCK_BLACKLIST.contains(m)).toList();
        // Fallback just in case someone empties the list via config changes
        List<Material> chosen = pool.isEmpty() ? mats : pool;
        return chosen.get(random.nextInt(chosen.size())).name();
    }

    private static String pickEntityName(List<EntityType> types) {
        // ensure we never accidentally pick wither/dragon
        EntityType t;
        do {
            t = types.get(random.nextInt(types.size()));
        } while (t == EntityType.WITHER || t == EntityType.ENDER_DRAGON);
        return t.name();
    }

    private static String pickBiome() {
        return SAFE_BIOMES.get(random.nextInt(SAFE_BIOMES.size()));
    }
}
