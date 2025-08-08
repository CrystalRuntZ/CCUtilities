package org.celestialcraft.cCUtilities.commands;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.ced.AutoDragonSpawner;
import org.celestialcraft.cCUtilities.modules.ced.DragonManager;
import org.celestialcraft.cCUtilities.modules.ced.DragonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CedCommand implements CommandExecutor, TabCompleter {

    private final DragonManager dragonManager;

    public CedCommand(DragonManager dragonManager) {
        this.dragonManager = dragonManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageConfig.mm("ced.usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> {
                if (!sender.hasPermission("customenderdragon.spawn")) {
                    sender.sendMessage(MessageConfig.mm("ced.no-permission"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageConfig.mm("ced.players-only"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(MessageConfig.mm("ced.spawn-usage"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("random")) {
                    if (!sender.hasPermission("ced.spawn.random")) {
                        sender.sendMessage(MessageConfig.mm("ced.no-permission-random"));
                        return true;
                    }

                    DragonType[] types = DragonType.values();
                    DragonType randomType = types[ThreadLocalRandom.current().nextInt(types.length)];
                    dragonManager.spawnDragon(randomType);
                    sender.sendMessage(MessageConfig.mm("ced.spawned-random").replaceText(builder ->
                            builder.matchLiteral("%type%").replacement(randomType.name())));
                    return true;
                }

                String input = args[1].toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
                Optional<DragonType> match = Arrays.stream(DragonType.values())
                        .filter(dt -> dt.name().equalsIgnoreCase(input))
                        .findFirst();

                if (match.isEmpty()) {
                    sender.sendMessage(MessageConfig.mm("ced.invalid-type").replaceText(builder ->
                            builder.matchLiteral("%input%").replacement(args[1])));
                    return true;
                }

                dragonManager.spawnDragon(match.get());
                sender.sendMessage(MessageConfig.mm("ced.spawned").replaceText(builder ->
                        builder.matchLiteral("%type%").replacement(match.get().name())));
                return true;
            }

            case "kill" -> {
                if (!sender.hasPermission("customenderdragon.kill")) {
                    sender.sendMessage(MessageConfig.mm("ced.no-permission-kill"));
                    return true;
                }

                if (dragonManager.killActiveDragon()) {
                    sender.sendMessage(MessageConfig.mm("ced.killed"));
                } else {
                    sender.sendMessage(MessageConfig.mm("ced.no-dragon"));
                }
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("customenderdragon.reload")) {
                    sender.sendMessage(MessageConfig.mm("ced.no-permission-reload"));
                    return true;
                }

                JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("cCUtilities");
                if (plugin == null) {
                    sender.sendMessage(MessageConfig.mm("ced.plugin-not-found"));
                    return true;
                }

                plugin.reloadConfig(); // Only reload plugin-level config.yml

                var ced = org.celestialcraft.cCUtilities.CCUtilities.getInstance().cedModule;
                ced.getConfig().reload(); // ✅ THIS reloads DragonConfig

                dragonManager.reload(); // reload dragon manager state if needed

                sender.sendMessage(MessageConfig.mm("ced.reloaded"));
                return true;
            }

            case "timer" -> {
                if (!sender.hasPermission("customenderdragon.timer")) {
                    sender.sendMessage(MessageConfig.mm("ced.no-permission-timer"));
                    return true;
                }

                long millis = AutoDragonSpawner.getTimeUntilNextSpawn();
                long minutes = millis / 60000;
                long seconds = (millis % 60000) / 1000;

                sender.sendMessage(MessageConfig.mm("ced.timer")
                        .replaceText(b -> b.matchLiteral("%minutes%").replacement(String.valueOf(minutes)))
                        .replaceText(b -> b.matchLiteral("%seconds%").replacement(String.valueOf(seconds))));
                return true;
            }

            case "fight" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageConfig.mm("ced.players-only"));
                    return true;
                }

                if (!player.hasPermission("customenderdragon.fight")) {
                    player.sendMessage(MessageConfig.mm("ced.no-permission-fight"));
                    return true;
                }

                EnderDragon dragon = dragonManager.getActiveDragon();
                if (dragon == null || !dragon.isValid()) {
                    player.sendMessage(MessageConfig.mm("ced.no-dragon"));
                    return true;
                }

                Location safe = findSafeGround(new Location(dragon.getWorld(), 0, 80, 0));
                player.teleportAsync(safe);
                player.sendMessage(MessageConfig.mm("ced.teleported"));
                return true;
            }

            default -> {
                sender.sendMessage(MessageConfig.mm("ced.usage"));
                return true;
            }
        }
    }

    private Location findSafeGround(Location base) {
        World world = base.getWorld();
        int x = base.getBlockX();
        int z = base.getBlockZ();

        int y = world.getHighestBlockYAt(x, z);
        Block ground = world.getBlockAt(x, y - 1, z);
        if (!ground.getType().isSolid()) y = 80;

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return Stream.of("spawn", "kill", "reload", "timer", "fight")
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> suggestions = Arrays.stream(DragonType.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (sender.hasPermission("customenderdragon.spawn.random")) {
                suggestions.add("random");
            }

            return suggestions.stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
