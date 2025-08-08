package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.randomkeys.RandomKeysModule;
import org.jetbrains.annotations.NotNull;

public class RandomKeysCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RandomKeysModule module;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public RandomKeysCommand(JavaPlugin plugin, RandomKeysModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("randomkeys")) return true;

        if (!sender.hasPermission("randomkeys.admin")) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.usage").replace("%label%", label)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.reloaded")));
                break;

            case "force":
                if (!module.isEnabled()) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.disabled")));
                    return true;
                }
                module.getKeyDistributor().giveKeyToRandomPlayer();
                sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.forced")));
                break;

            default:
                sender.sendMessage(mini.deserialize(MessageConfig.get("randomkeys.unknown-subcommand")));
                break;
        }

        return true;
    }
}
