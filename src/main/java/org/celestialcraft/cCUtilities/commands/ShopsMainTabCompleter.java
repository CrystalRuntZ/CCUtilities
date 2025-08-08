package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShopsMainTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("claim", "define", "trust", "untrust", "info", "unclaim", "setwarp", "warp"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
                return filter(new ArrayList<>(getOnlinePlayerNames()), args[1]);
            }

            if (args[0].equalsIgnoreCase("warp")) {
                return filter(new ArrayList<>(getOnlinePlayerNames()), args[1]);
            }

            if (args[0].equalsIgnoreCase("define")) {
                return Collections.singletonList("<name>");
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }

    private Set<String> getOnlinePlayerNames() {
        Set<String> names = new HashSet<>();
        Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
        return names;
    }
}
