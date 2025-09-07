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
import java.util.concurrent.locks.ReentrantLock;

public class KeyDistributor {
    private final JavaPlugin plugin;
    private final KeyDataManager keyDataManager;

    // Tracks players who have received a key within the current hour (instance-local)
    private final Set<UUID> distributedThisHour = new HashSet<>();
    private long localHourBucket = Long.MIN_VALUE;

    // Schedules
    private int hourlyTaskId = -1;

    private final MiniMessage mini = MiniMessage.miniMessage();

    // JVM-wide guard
    private static final Object GLOBAL_HOUR_LOCK = new Object();
    private static long globalLastIssuedHour = Long.MIN_VALUE;

    // Re-entrance guard + hard lock for issuance section
    private final ReentrantLock issueLock = new ReentrantLock();
    private volatile boolean distributing = false;

    public KeyDistributor(JavaPlugin plugin, KeyDataManager keyDataManager) {
        this.plugin = plugin;
        this.keyDataManager = keyDataManager;
    }

    public void scheduleHourlyDistribution() {
        if (hourlyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(hourlyTaskId);
        }

        long delaySeconds = Duration.between(
                Instant.now(),
                Instant.now().atZone(ZoneOffset.UTC).plusHours(1).truncatedTo(ChronoUnit.HOURS).toInstant()
        ).getSeconds();

        hourlyTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                giveKeyToRandomPlayer();
            }
        }, Math.max(0, delaySeconds) * 20, 3600L * 20).getTaskId();
    }

    public void scheduleDailyReset() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextReset = now.toLocalDate().plusDays(1).atStartOfDay();
        long delaySeconds = Duration.between(now, nextReset).getSeconds();

        new BukkitRunnable() {
            @Override
            public void run() {
                keyDataManager.resetDaily();
                distributedThisHour.clear();
                // localHourBucket will roll forward automatically on next give
            }
        }.runTaskTimer(plugin, Math.max(0, delaySeconds) * 20, 86400L * 20);
    }

    /**
     * Issue at most one key per UTC hour.
     * Steps:
     *  1) Build candidate list (cheap, outside lock).
     *  2) If none, bail quietly.
     *  3) Enter critical section:
     *     - Re-check hour
     *     - Check & set both in-JVM and persisted hour stamp
     *  4) Issue key to chosen player
     */
    public void giveKeyToRandomPlayer() {
        if (!plugin.getConfig().getBoolean("random-keys.enabled", true)) return;

        final long hourBucket = Instant.now().getEpochSecond() / 3600L;

        // Local hour rollover
        if (localHourBucket != hourBucket) {
            distributedThisHour.clear();
            localHourBucket = hourBucket;
        }

        // Instance re-entrance guard (cheap fast-path)
        if (distributing) return;

        // Build candidates (outside the locks)
        final long activeWindowMs = plugin.getConfig().getInt("random-keys.active-time-minutes", 5) * 60 * 1000L;

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        List<Player> activePlayers = online.stream()
                .filter(p -> ActivityTracker.isRecentlyActive(p, activeWindowMs))
                .collect(Collectors.toList());

        List<Player> candidates = !activePlayers.isEmpty() ? activePlayers : online;

        List<Player> notReceived = candidates.stream()
                .filter(p -> !keyDataManager.hasReceivedKey(p.getUniqueId()))
                .collect(Collectors.toList());

        List<Player> eligible = !notReceived.isEmpty() ? notReceived : candidates;

        // Exclude those we already gave to in this hour (instance-local)
        List<Player> hourEligible = eligible.stream()
                .filter(p -> !distributedThisHour.contains(p.getUniqueId()))
                .toList();

        if (hourEligible.isEmpty()) {
            return; // Do NOT claim the hour; let a later attempt this hour try again.
        }

        // Pick winner (still outside the critical section)
        Player chosen = hourEligible.get((int) (Math.random() * hourEligible.size()));

        // Enter strict issuance section
        if (!issueLock.tryLock()) return; // another issuer in progress in this instance
        distributing = true;
        try {
            // Double-check hour claim under both a JVM-wide lock and with a persisted stamp.
            synchronized (GLOBAL_HOUR_LOCK) {
                long persisted = plugin.getConfig().getLong("random-keys.last-issued-hour", Long.MIN_VALUE);

                if (globalLastIssuedHour == hourBucket || persisted == hourBucket) {
                    return; // already issued this hour (in-JVM or persisted)
                }

                // Claim the hour atomically: write to config and set in-memory marker
                plugin.getConfig().set("random-keys.last-issued-hour", hourBucket);
                plugin.saveConfig();
                globalLastIssuedHour = hourBucket;
            }

            // Mark chosen so this instance doesn't pick them again this hour
            distributedThisHour.add(chosen.getUniqueId());

            String keyName = plugin.getConfig().getString("random-keys.key-name", "defaultkey");

            // Issue the key
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "crates key give " + chosen.getName() + " " + keyName + " 1");
            plugin.getLogger().info("RandomKeys: Given " + keyName + " to " + chosen.getName());

            // Broadcast
            String rawMessage = MessageConfig.get("randomkeys.broadcast");
            if (rawMessage != null && !rawMessage.isBlank()) {
                Component broadcast = mini.deserialize(rawMessage, Placeholder.unparsed("player", chosen.getName()));
                Bukkit.broadcast(broadcast);
            }

            // Daily gate
            keyDataManager.markReceived(chosen.getUniqueId());

        } finally {
            distributing = false;
            issueLock.unlock();
        }
    }
}
