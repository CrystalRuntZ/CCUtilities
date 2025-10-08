package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RawBroadcastCommand implements CommandExecutor {
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender) ||
                !sender.hasPermission("ccutilities.rawbc")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§eUsage: /rawbc <message>");
            return true;
        }

        String message = String.join(" ", args);
        Component component;

        // Detect legacy formatting (&/§)
        if (message.matches(".*[&§][0-9a-fk-or].*")) {
            component = legacy.deserialize(message);
        } else {
            component = mini.deserialize(message);
        }

        Bukkit.getServer().broadcast(component);
        return true;
    }
}
