package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.activity.CelestialShopManager;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

public class CelestialShopCommand implements CommandExecutor {

    private final CelestialShopManager shopManager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public CelestialShopCommand(CelestialShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("activity")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("activity-reward.player-only")));
            return true;
        }

        shopManager.openShop(player);
        return true;
    }
}
