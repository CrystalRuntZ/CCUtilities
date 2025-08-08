package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CelestialPointsAdminTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName) // replaced lambda with method reference
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if ("<amount>".startsWith(args[1].toLowerCase())) {
                return Collections.singletonList("<amount>");
            }
            return new ArrayList<>();
        } else {
            return Collections.emptyList();
        }
    }
}
