package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;

import java.util.*;

public class BiomeListener implements Listener {

    private final Map<UUID, Set<NamespacedKey>> visitedBiomes = new HashMap<>();

    @EventHandler
    public void onBiomeChange(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        var biomeKey = player.getLocation().getBlock().getBiome().getKey();
        UUID uuid = player.getUniqueId();

        Set<NamespacedKey> visited = visitedBiomes.computeIfAbsent(uuid, k -> new HashSet<>());
        if (visited.add(biomeKey)) {
            for (Quest quest : QuestManager.getQuests(player)) {
                if (quest.getType() == QuestType.DISCOVER_BIOME && !quest.isComplete() && !quest.isExpired()) {
                    quest.setProgress(quest.getProgress() + 1);

                    var inventory = player.getInventory();
                    for (var item : inventory.getContents()) {
                        if (item == null) continue;
                        var questId = LoreUtils.getQuestId(item);
                        if (questId != null && questId.equals(quest.getId())) {
                            LoreUtils.updateLore(item, quest);
                        }
                    }

                    LoreUtils.sendProgressActionBar(player, quest);
                }
            }
        }
    }
}
