package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopSelectionStorage;
import org.celestialcraft.cCUtilities.utils.ShopUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShopsMainCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull[] args) {
        if (!ModuleManager.isEnabled("playershops")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(mm.deserialize("<red>Usage: /shops <trust|untrust|claim|unclaim|define|info|setwarp|warp|wand>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        ShopRegion region = ShopDataManager.getRegionAt(player.getLocation());

        switch (sub) {
            case "wand" -> {
                if (!player.isOp()) {
                    player.sendMessage(mm.deserialize("<red>You do not have permission to use this command."));
                    return true;
                }

                ItemStack wand = new ItemStack(Material.STONE_HOE);
                ItemMeta meta = wand.getItemMeta();
                meta.displayName(Component.text("Shop Wand", NamedTextColor.AQUA));
                wand.setItemMeta(meta);

                player.getInventory().addItem(wand);
                player.sendMessage(mm.deserialize("<green>Shop Wand given."));
                return true;
            }
            case "trust" -> {
                if (args.length == 2) {
                    if (region == null || !region.isOwner(player.getUniqueId())) {
                        player.sendMessage(mm.deserialize("<red>You must be in your own shop to trust someone."));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(mm.deserialize("<red>That player is not online."));
                        return true;
                    }
                    if (ShopDataManager.isDivine(region) && !target.hasPermission("shops.divine")) {
                        player.sendMessage(mm.deserialize("<red>That player does not have permission to be trusted in a divine shop."));
                        return true;
                    }
                    Set<UUID> trusted = ShopDataManager.getTrusted(region.name());
                    if (trusted.contains(target.getUniqueId())) {
                        player.sendMessage(mm.deserialize("<gray>" + target.getName() + " is already trusted."));
                        return true;
                    }
                    ShopDataManager.addTrusted(region.name(), target.getUniqueId());
                    player.sendMessage(mm.deserialize("<green>" + target.getName() + " has been trusted."));
                } else if (args.length == 1) {
                    if (region == null || !region.isOwner(player.getUniqueId())) {
                        player.sendMessage(mm.deserialize("<red>You must be in your shop to view trusted players."));
                        return true;
                    }
                    Set<UUID> trusted = ShopDataManager.getTrusted(region.name());
                    if (trusted.isEmpty()) {
                        player.sendMessage(mm.deserialize("<gray>No trusted players."));
                    } else {
                        player.sendMessage(mm.deserialize("<gold>Trusted Players:"));
                        for (UUID uuid : trusted) {
                            String name = Bukkit.getOfflinePlayer(uuid).getName();
                            player.sendMessage(mm.deserialize("<yellow>- " + (name != null ? name : uuid)));
                        }
                    }
                } else {
                    player.sendMessage(mm.deserialize("<red>Usage: /shops trust <player> or /shops trust list"));
                }
                return true;
            }
            case "untrust" -> {
                if (region == null || !region.isOwner(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize("<red>You must be in your own shop to untrust someone."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>Usage: /shops untrust <player>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (ShopDataManager.isTrusted(region.name(), target.getUniqueId())) {
                    ShopDataManager.removeTrusted(region.name(), target.getUniqueId());
                    player.sendMessage(mm.deserialize("<green>" + target.getName() + " has been untrusted."));
                } else {
                    player.sendMessage(mm.deserialize("<gray>" + target.getName() + " was not trusted."));
                }
                return true;
            }
            case "info" -> {
                if (region == null) {
                    player.sendMessage(mm.deserialize("<red>You must be standing in a shop region."));
                    return true;
                }
                if (!ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize("<red>This shop is not claimed."));
                    return true;
                }
                UUID ownerUUID = ShopDataManager.getOwnerUUID(region.name());
                String ownerName = ownerUUID == null ? "Unknown" : Bukkit.getOfflinePlayer(ownerUUID).getName();
                long lastUpdated = ShopDataManager.getLastUpdated(region.name()) != null ? ShopDataManager.getLastUpdated(region.name()) : 0L;
                long timeAgo = System.currentTimeMillis() - lastUpdated;
                long days = timeAgo / (1000 * 60 * 60 * 24);
                long minutes = (timeAgo / (1000 * 60)) % 60;
                player.sendMessage(mm.deserialize("<gray>Shop Owner: <gold>" + ownerName));
                player.sendMessage(mm.deserialize("<gray>Last Updated: <gold>" + days + " days, " + minutes + " minutes ago"));
                return true;
            }
            case "claim" -> {
                if (region == null) {
                    player.sendMessage(mm.deserialize("<red>You must be in a defined shop area to claim it."));
                    return true;
                }
                if (ShopDataManager.hasClaimed(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize("<red>You already own a shop."));
                    return true;
                }
                if (ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize("<red>This shop is already claimed."));
                    return true;
                }
                if (ShopDataManager.isDivine(region) && !player.hasPermission("shops.divine")) {
                    player.sendMessage(mm.deserialize("<red>You do not have permission to claim a divine shop."));
                    return true;
                }
                ShopDataManager.claimShop(player.getUniqueId(), region.name());
                player.sendMessage(mm.deserialize("<green>You have successfully claimed this shop."));
                return true;
            }
            case "unclaim" -> {
                if (!player.hasPermission("shops.admin")) {
                    player.sendMessage(mm.deserialize("<red>You do not have permission to do that."));
                    return true;
                }
                if (region == null || !ShopDataManager.isClaimed(region.name())) {
                    player.sendMessage(mm.deserialize("<red>You must be standing in a claimed shop to unclaim it."));
                    return true;
                }
                if (ShopDataManager.unclaimShopAt(player.getLocation())) {
                    ShopSelectionStorage.clear(player.getUniqueId()); // ← clear selection here
                    player.sendMessage(mm.deserialize("<green>Shop unclaimed successfully."));
                } else {
                    player.sendMessage(mm.deserialize("<red>Failed to unclaim shop."));
                }
                return true;
            }
            case "define" -> {
                if (!player.isOp()) {
                    player.sendMessage(mm.deserialize("<red>Only operators can define shops."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>Usage: /shops define <name>"));
                    return true;
                }
                ShopUtils.defineShop(player, args[1]);
                return true;
            }
            case "setwarp" -> {
                if (region == null || !region.isOwner(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize("<red>You must be in your shop to set the warp."));
                    return true;
                }
                Location loc = player.getLocation();
                ShopDataManager.setWarp(region.name(), loc);
                player.sendMessage(mm.deserialize("<green>Shop warp set."));
                return true;
            }
            case "warp" -> {
                if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
                    Location warp = ShopDataManager.getRandomWarp();
                    if (warp == null) {
                        player.sendMessage(mm.deserialize("<red>No shop warps are currently set."));
                        return true;
                    }
                    player.teleportAsync(warp).thenRun(() ->
                            player.sendMessage(mm.deserialize("<green>Teleported to a random shop!"))
                    );
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>Usage: /shops warp <player>|random"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String shop = ShopDataManager.getClaim(target.getUniqueId());
                if (shop == null || ShopDataManager.getWarp(shop) == null) {
                    player.sendMessage(mm.deserialize("<red>That player has no shop warp set."));
                    return true;
                }
                player.teleport(ShopDataManager.getWarp(shop));
                player.sendMessage(mm.deserialize("<green>Teleported to " + target.getName() + "'s shop."));
                return true;
            }
            default -> {
                player.sendMessage(mm.deserialize("<red>Unknown subcommand."));
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull[] args) {
        if (args.length == 1) {
            return Stream.of("trust", "untrust", "info", "claim", "unclaim", "define", "setwarp", "warp", "wand")
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("untrust")) {
            if (sender instanceof Player player) {
                ShopRegion region = ShopDataManager.getRegionAt(player.getLocation());
                if (region != null && region.isOwner(player.getUniqueId())) {
                    return ShopDataManager.getTrusted(region.name()).stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(Objects::nonNull)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("warp")) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            names.add("random");
            return names;
        }

        return Collections.emptyList();
    }
}
