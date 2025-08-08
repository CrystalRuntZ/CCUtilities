package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.rtp.CooldownManager;
import org.celestialcraft.cCUtilities.modules.rtp.RtpHandler;
import org.jetbrains.annotations.NotNull;

public class RtpCommand implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;

    public RtpCommand() {
        this.plugin = JavaPlugin.getPlugin(CCUtilities.class);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("rtp")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("rtp.messages.only-player")));
            return true;
        }

        String type = switch (label.toLowerCase()) {
            case "wild" -> "overworld";
            case "nether" -> "nether";
            case "end" -> "end";
            case "rtp" -> (args.length > 0) ? args[0].toLowerCase() : null;
            default -> null;
        };

        if (!"overworld".equals(type) && !"nether".equals(type) && !"end".equals(type)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("rtp.messages.usage")));
            return true;
        }

        if (CooldownManager.isOnCooldown(player.getUniqueId(), type)) {
            String timeLeft = CooldownManager.formatTimeLeft(player.getUniqueId(), type);
            String msg = MessageConfig.get("rtp.messages.on-cooldown")
                    .replace("%world%", type)
                    .replace("%time%", timeLeft);
            player.sendMessage(mm.deserialize(msg));
            return true;
        }

        Location location = RtpHandler.findSafeLocation(type);
        if (location == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("rtp.messages.fail")));
            return true;
        }

        player.teleport(location);
        String msg = MessageConfig.get("rtp.messages.success").replace("%world%", type);
        player.sendMessage(mm.deserialize(msg));

        long seconds = plugin.getConfig().getLong("rtp.cooldown-seconds", 120);
        CooldownManager.setCooldown(player.getUniqueId(), type, seconds);
        return true;
    }
}
