package org.celestialcraft.cCUtilities.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BrigadierWrapperCommand extends Command {
    private final CommandExecutor executor;
    private final TabCompleter completer;

    public BrigadierWrapperCommand(String name, CommandExecutor executor, TabCompleter completer) {
        super(name);
        this.executor = executor;
        this.completer = completer;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String @NotNull [] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args) {
        if (completer != null) {
            return Objects.requireNonNull(completer.onTabComplete(sender, this, alias, args));
        }
        return Collections.emptyList();
    }
}
