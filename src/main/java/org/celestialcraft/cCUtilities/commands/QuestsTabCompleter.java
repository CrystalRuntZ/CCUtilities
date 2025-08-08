package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.modules.quests.config.QuestConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestsTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("quests.give")) options.add("give");
            if (sender.hasPermission("quests.reload")) options.add("reload");
            if (sender.hasPermission("quests.weekly")) options.add("weekly");
            if (sender.hasPermission("quests.claim")) options.add("claim");
            if (sender.hasPermission("quests.list")) options.add("list");

            return options.stream()
                    .filter(opt -> opt.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("quests.give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("quests.give")) {
            return QuestConfig.getAllTemplates().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
