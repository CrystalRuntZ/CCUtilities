package org.celestialcraft.cCUtilities.modules.quests.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;
import org.celestialcraft.cCUtilities.modules.quests.util.LoreUtils;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestProgress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwimListener implements Listener {

    // carry over sub-1.0 distances so bobbing still counts
    private static final Map<UUID, Double> residualByPlayer = new HashMap<>();

    @EventHandler
    public void onSwim(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("quests")) return;

        var player = event.getPlayer();
        var to = event.getTo();
        var from = event.getFrom();
        if (from.getWorld() != to.getWorld()) return;

        // exclude riding boats
        if (player.isInsideVehicle() && player.getVehicle() instanceof Boat) return;

        // must be in/at water (feet or eyes) to count
        if (!isInOrAtWater(player.getLocation().getBlock())
                && !isInOrAtWater(player.getEyeLocation().getBlock())) {
            return;
        }

        // distance this tick (3D so vertical swimming/bobbing counts)
        double delta = from.toVector().distance(to.toVector());
        if (delta <= 0) return;

        UUID id = player.getUniqueId();
        double sum = residualByPlayer.getOrDefault(id, 0.0) + delta;

        int whole = (int) Math.floor(sum);
        if (whole <= 0) {
            residualByPlayer.put(id, sum);
            return;
        }
        residualByPlayer.put(id, sum - whole);

        // 1) Weekly bundle path first
        boolean handled = QuestProgress.get().addProgress(player, QuestType.SWIM_DISTANCE, whole);
        if (handled) return;

        // 2) Fallback: single-quest items path
        List<Quest> quests = QuestManager.getQuests(player);
        for (Quest quest : quests) {
            if (quest.getType() == QuestType.SWIM_DISTANCE && !quest.isComplete() && !quest.isExpired()) {
                quest.setProgress(quest.getProgress() + whole);

                var inv = player.getInventory();
                for (var item : inv.getContents()) {
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

    private boolean isInOrAtWater(Block block) {
        if (block.getType() == Material.WATER) return true;
        BlockData data = block.getBlockData();
        return data instanceof Waterlogged w && w.isWaterlogged();
    }
}
