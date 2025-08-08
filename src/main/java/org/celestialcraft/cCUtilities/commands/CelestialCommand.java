package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.jetbrains.annotations.NotNull;

public class CelestialCommand implements CommandExecutor {

    private final CCUtilities main;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CelestialCommand(CCUtilities main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("core.usage")));
            return true;
        }

        if (!sender.hasPermission("celestial.reload")) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("core.no-permission")));
            return true;
        }

        sender.sendMessage(mm.deserialize(MessageConfig.get("core.reloading")));

        try {
            main.reloadAll();
            sender.sendMessage(mm.deserialize(MessageConfig.get("core.reloaded")));
        } catch (Exception e) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("core.reload-error")));
            main.getLogger().severe("An error occurred while reloading CCUtilities:");
            main.getLogger().severe(e.toString());
            for (StackTraceElement element : e.getStackTrace()) {
                main.getLogger().severe("  at " + element.toString());
            }
        }

        return true;
    }
}
