package org.celestialcraft.cCUtilities.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegistrar {

    public static void register(JavaPlugin plugin, String name, CommandExecutor executor, TabCompleter completer) {
        var command = new BrigadierWrapperCommand(name, executor, completer);
        Bukkit.getCommandMap().register(plugin.getName().toLowerCase(), command);
    }
}
