package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.modules.customitems.UnityItem;
import org.celestialcraft.cCUtilities.modules.customitems.UnityRequest;
import org.celestialcraft.cCUtilities.modules.customitems.UnityRequestManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UnityAcceptCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || args.length == 0) return true;

        UUID senderUUID = player.getUniqueId();

        UUID fromUUID;
        try {
            fromUUID = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            return true;
        }

        UnityRequest request = UnityRequestManager.requests.remove(senderUUID);
        if (request == null || !request.from().equals(fromUUID)) return true;

        Player fromPlayer = Bukkit.getPlayer(fromUUID);
        if (fromPlayer == null) return true;

        fromPlayer.getInventory().removeItemAnySlot(request.originalItem());

        ItemStack linkedItem = UnityItem.createLinkedItem(fromPlayer.getName(), player.getName(), request.originalItem());

        fromPlayer.getInventory().addItem(linkedItem.clone());
        player.getInventory().addItem(linkedItem);

        fromPlayer.sendMessage("§7You have united with " + player.getName() + ".");
        player.sendMessage("§7You have united with " + fromPlayer.getName() + ".");

        playHeartBurst(fromPlayer);
        playHeartBurst(player);


        return true;
    }

    private void playHeartBurst(Player player) {
        for (double t = 0; t < Math.PI; t += Math.PI / 16) {
            double x = 0.5 * Math.sin(t) * Math.sin(t) * Math.cos(t);
            double z = 0.5 * Math.sin(t) * Math.sin(t) * Math.sin(t);
            double y = 0.5 * Math.cos(t);

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    player.getLocation().add(x, 2 + y, z),
                    0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 70, 70), 1.5f)
            );
        }
    }
}
