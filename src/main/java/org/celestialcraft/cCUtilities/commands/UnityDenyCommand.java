package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.modules.customitems.UnityRequestManager;
import org.celestialcraft.cCUtilities.modules.customitems.UnityRequest;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UnityDenyCommand implements CommandExecutor {

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
        if (fromPlayer != null) {
            fromPlayer.sendMessage("§7☆ " + player.getName() + " has declined your invitation to unite.");
        }

        player.sendMessage("§7You declined the invitation.");
        return true;
    }
}
