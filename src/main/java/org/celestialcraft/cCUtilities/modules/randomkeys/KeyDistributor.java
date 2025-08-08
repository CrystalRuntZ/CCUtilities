package org.celestialcraft.cCUtilities.modules.randomkeys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.utils.ActivityTracker;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class KeyDistributor {
    private final JavaPlugin plugin;
    private final KeyDataManager keyDataManager;
    private final Set<UUID> distributedThisHour = new HashSet<>();
    private int hourlyTaskId = -1;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public KeyDistributor(JavaPlugin plugin, KeyDataManager keyDataManager) {
        this.plugin = plugin;
        this.keyDataManager = keyDataManager;
    }

    public void scheduleHourlyDistribution() {
        if (hourlyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(hourlyTaskId);
        }

        long delay = Duration.between(
                Instant.now(),
                Instant.now().atZone(ZoneOffset.UTC).plusHours(1).truncatedTo(ChronoUnit.HOURS).toInstant()
        ).getSeconds();

        hourlyTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                giveKeyToRandomPlayer();
            }
        }, delay * 20, 3600 * 20).getTaskId();
    }

    public void scheduleDailyReset() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextReset = now.toLocalDate().plusDays(1).atStartOfDay();
        long delay = Duration.between(now, nextReset).getSeconds();

        new BukkitRunnable() {
            @Override
            public void run() {
                keyDataManager.resetDaily();
                distributedThisHour.clear();
            }
        }.runTaskTimer(plugin, delay * 20, 86400 * 20);
    }

    public void giveKeyToRandomPlayer() {
        if (!plugin.getConfig().getBoolean("random-keys.enabled", true)) return;

        long activeWindow = plugin.getConfig().getInt("random-keys.active-time-minutes", 5) * 60 * 1000L;
        List<Player> activePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> ActivityTracker.isRecentlyActive(p, activeWindow))
                .collect(Collectors.toList());

        List<Player> candidates = !activePlayers.isEmpty() ? activePlayers : List.copyOf(Bukkit.getOnlinePlayers());
        if (candidates.isEmpty()) return;

        List<Player> notReceived = candidates.stream()
                .filter(p -> !keyDataManager.hasReceivedKey(p.getUniqueId()))
                .collect(Collectors.toList());

        List<Player> eligible = !notReceived.isEmpty() ? notReceived : candidates;

        Player chosen = eligible.get((int) (Math.random() * eligible.size()));

        if (distributedThisHour.contains(chosen.getUniqueId())) return;
        distributedThisHour.add(chosen.getUniqueId());

        String keyName = plugin.getConfig().getString("random-keys.key-name", "defaultkey");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crates key give " + chosen.getName() + " " + keyName + " 1");
        plugin.getLogger().info("RandomKeys: Given " + keyName + " to " + chosen.getName());

        String rawMessage = MessageConfig.get("randomkeys.broadcast");
        if (!rawMessage.isBlank()) {
            Component broadcast = mini.deserialize(rawMessage, Placeholder.unparsed("player", chosen.getName()));
            Bukkit.broadcast(broadcast);
        }

        keyDataManager.markReceived(chosen.getUniqueId());
    }
}
