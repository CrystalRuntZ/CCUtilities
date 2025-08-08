package org.celestialcraft.cCUtilities.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EntityManagerTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 0) return Collections.emptyList();

        return switch (args.length) {
            case 1 -> {
                List<String> subcommands = List.of("list", "limit", "limitblock", "reload", "viewlimits");
                yield filterStartsWith(subcommands, args[0]);
            }

            case 2 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "limit" -> {
                    List<String> entityNames = new ArrayList<>();
                    for (EntityType type : EntityType.values()) {
                        entityNames.add(type.name().toLowerCase(Locale.ROOT));
                    }
                    yield filterStartsWith(entityNames, args[1]);
                }
                case "limitblock" -> {
                    List<String> blockNames = new ArrayList<>();
                    for (Material material : Material.values()) {
                        if (material.isBlock()) {
                            blockNames.add(material.name().toLowerCase(Locale.ROOT));
                        }
                    }
                    yield filterStartsWith(blockNames, args[1]);
                }
                default -> Collections.emptyList();
            };

            case 3 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "limit", "limitblock" -> "<amount>".startsWith(args[2])
                        ? List.of("<amount>")
                        : Collections.emptyList();
                default -> Collections.emptyList();
            };

            default -> Collections.emptyList();
        };
    }

    private List<String> filterStartsWith(List<String> list, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String item : list) {
            if (item.startsWith(lower)) {
                result.add(item);
            }
        }
        return result;
    }
}
