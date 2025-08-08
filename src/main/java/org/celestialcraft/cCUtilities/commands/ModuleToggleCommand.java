package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleToggleCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ModuleToggleCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("modules.no-permission")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("modules.usage")));
            return true;
        }

        String action = args[0].toLowerCase();
        String name = args[1].toLowerCase();

        switch (action) {
            case "enable" -> {
                if (ModuleManager.enable(name)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("modules.enabled")
                            .replace("%module%", name)));
                    plugin.getLogger().info(sender.getName() + " enabled module: " + name);
                } else {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("modules.enable-fail")));
                }
            }
            case "disable" -> {
                if (ModuleManager.disable(name)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("modules.disabled")
                            .replace("%module%", name)));
                    plugin.getLogger().info(sender.getName() + " disabled module: " + name);
                } else {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("modules.disable-fail")));
                }
            }
            default -> sender.sendMessage(mini.deserialize(MessageConfig.get("modules.usage")));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("enable", "disable");
        }
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return ModuleManager.getModules().stream()
                    .map(Module::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }
        return Collections.emptyList();
    }
}
