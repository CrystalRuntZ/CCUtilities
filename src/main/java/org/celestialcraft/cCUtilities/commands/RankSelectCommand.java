package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RankSelectCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final Map<String, String> PREFIXES = new LinkedHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_MAP = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 60 * 1000;

    static {
        PREFIXES.put("architect", "&#7C1F58&lA&#93306D&lR&#A94181&lC&#C05296&lH&#D663AA&lI&#C05296&lT&#A94181&lE&#93306D&lC&#7C1F58&lT &7: &#D663AA");
        PREFIXES.put("designer", "&#ECDF8E&lD&#EADE95&lE&#E7DD9B&lS&#E5DBA2&lI&#E2DAA8&lG&#E5DC9F&lN&#E9DD97&lE&#ECDF8E&lR &7: &#E2DAA8");
        PREFIXES.put("ethereal", "&#EDC5FF&lE&#F2D8EE&lT&#F9C8DC&lH&#FFB7CA&lE&#FBC2D6&lR&#F6CDE2&lE&#F2D8EE&lA&#EDC5FF&lL &7: &#FFB7CA");
        PREFIXES.put("ascendant", "&#FFB7CA&lA&#FF8E8E&lS&#FBA08B&lC&#F7B187&lE&#F3C384&lN&#F9AA8A&lD&#FF9090&lA&#FFA4AD&lN&#FFB7CA&lT &7: &#FF9090");
        PREFIXES.put("radiant", "&#F2D8EE&lR&#E0EBD7&lA&#CDFEBF&lD&#B2EEA1&lI&#CDFEBF&lA&#E0EBD7&lN&#F2D8EE&lT &7: &#B2EEA1");
        PREFIXES.put("heavenly", "&#FFF6E3&lH&#F9EFC2&lE&#F4E8A0&lA&#EEE07F&lV&#E8D95D&lE&#F0E38A&lN&#F7ECB6&lL&#FFF6E3&lY &7: &#E8D95D");
        PREFIXES.put("jupiter", "&#F38177&lJUPITER &7: &#F38177");
        PREFIXES.put("saturn", "&#E6BB65&lSATURN &7: &#E6BB65");
        PREFIXES.put("neptune", "&#FBEA8D&lNEPTUNE &7: &#FBEA8D");
        PREFIXES.put("earth", "&#C9FB8D&lEARTH &7: &#C9FB8D");
        PREFIXES.put("venus", "&#7AE4A6&lVENUS &7: &#7AE4A6");
        PREFIXES.put("mars", "&#638EEA&LMARS &7: &#638EEA");
        PREFIXES.put("mercury", "&#B6C2FF&lMERCURY &7: &#B6C2FF");
        PREFIXES.put("pluto", "&#FFFDFF&lPLUTO &7: &#FFFDFF");
        PREFIXES.put("star", "&7&lSTAR &7: &7");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("rankselect")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessageConfig.get("rankselect.console-only")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(mm.deserialize(MessageConfig.get("rankselect.usage")));
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUsed = COOLDOWN_MAP.getOrDefault(uuid, 0L);
        long remaining = (lastUsed + COOLDOWN_MILLIS) - now;

        if (remaining > 0) {
            long seconds = remaining / 1000;
            player.sendMessage(mm.deserialize(MessageConfig.get("rankselect.cooldown").replace("%seconds%", String.valueOf(seconds))));
            return true;
        }

        String arg = args[0].toLowerCase();

        if (arg.equals("clear")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta clear");
            player.sendMessage(mm.deserialize(MessageConfig.get("rankselect.cleared")));
            COOLDOWN_MAP.put(uuid, now);
            return true;
        }

        if (!PREFIXES.containsKey(arg)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("rankselect.invalid-rank")));
            return true;
        }

        if (!arg.equals("star") && !player.hasPermission("ccutilities.rankselect." + arg)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("rankselect.no-permission")));
            return true;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta clear");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta addprefix 999 \"" + PREFIXES.get(arg) + "\"");

        COOLDOWN_MAP.put(uuid, now);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();

        String input = args[0].toLowerCase();
        List<String> results = new ArrayList<>();

        if ("clear".startsWith(input)) results.add("clear");

        for (String rank : PREFIXES.keySet()) {
            if (rank.startsWith(input)) {
                if (rank.equals("star") || player.hasPermission("ccutilities.rankselect." + rank)) {
                    results.add(rank);
                }
            }
        }
        return results;
    }
}
