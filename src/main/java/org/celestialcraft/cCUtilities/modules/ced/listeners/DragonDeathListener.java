package org.celestialcraft.cCUtilities.modules.ced.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.RewardDistributor;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DragonDeathListener implements Listener {

    private final DragonManager dragonManager;
    private final RewardDistributor rewardDistributor;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DragonDeathListener(DragonManager dragonManager, RewardDistributor rewardDistributor) {
        this.dragonManager = dragonManager;
        this.rewardDistributor = rewardDistributor;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;

        UUID id = dragon.getUniqueId();
        UUID expectedId = dragonManager.getActiveDragonId();

        Bukkit.getLogger().info("[CED] DragonDeathListener triggered.");
        Bukkit.getLogger().info("[CED] Dragon died. UUID: " + id);
        Bukkit.getLogger().info("[CED] Active dragon UUID: " + expectedId);

        if (!id.equals(expectedId)) {
            Bukkit.getLogger().warning("[CED] Skipping reward distribution. Dragon UUID does not match active dragon.");
            return;
        }

        var tracker = dragonManager.getDamageTracker();
        DragonType type = dragonManager.getActiveDragonType();

        List<Map.Entry<UUID, Double>> top = tracker.getTopDamagers(id, 3);
        if (top.isEmpty()) {
            Bukkit.getLogger().warning("[CED] No damagers found for dragon: " + id);
        }

        for (Map.Entry<UUID, Double> entry : top) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                int rounded = (int) Math.round(entry.getValue());
                String msg = MessageConfig.get("ced.damage-dealt")
                        .replace("{amount}", String.valueOf(rounded));
                player.sendMessage(mm.deserialize(msg));
                Bukkit.getLogger().info("[CED] " + player.getName() + " dealt " + rounded + " damage.");
            }
        }

        rewardDistributor.distribute(dragon, type, id);
        dragonManager.clearActiveDragon();
    }
}
