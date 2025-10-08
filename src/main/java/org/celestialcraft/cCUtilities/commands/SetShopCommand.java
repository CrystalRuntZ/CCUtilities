package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SetShopCommand implements CommandExecutor {
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("playershops")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("playershops.only-players")));
            return true;
        }
        ShopRegion region = ShopDataManager.getRegionAt(player.getLocation());
        UUID uid = player.getUniqueId();
        if (region == null || !uid.equals(ShopDataManager.getOwnerUUID(region.name()))) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.setwarp-missing-claim")));
            return true;
        }
        Location loc = player.getLocation();
        ShopDataManager.setWarp(region.name(), loc);
        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.setwarp-success")));
        return true;
    }
}
