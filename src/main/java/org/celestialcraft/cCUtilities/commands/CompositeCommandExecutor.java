package org.celestialcraft.cCUtilities.commands;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompositeCommandExecutor implements CommandExecutor, TabCompleter {

    private final CommandExecutor[] executors;

    public CompositeCommandExecutor(CommandExecutor... executors) {
        this.executors = executors;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        for (CommandExecutor executor : executors) {
            if (executor.onCommand(sender, command, label, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        for (CommandExecutor executor : executors) {
            if (executor instanceof TabCompleter completer) {
                List<String> result = completer.onTabComplete(sender, command, alias, args);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        }
        return List.of();
    }
}
