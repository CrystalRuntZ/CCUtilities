package org.celestialcraft.cCUtilities.modules.celestialvoting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.celestialvoting.config.VotingConfig;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Logger;

public class VoteStreakTracker {

    private static Connection connection;
    private static final Logger logger = CCUtilities.getPlugin(CCUtilities.class).getLogger();
    private static VotingConfig votingConfig;

    public static void setVotingConfig(VotingConfig config) {
        votingConfig = config;
    }

    public static void initialize(File dataFolder) {
        try {
            File dbFile = new File(dataFolder, "voting_streaks.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vote_streaks (
                        uuid TEXT PRIMARY KEY,
                        streak INTEGER NOT NULL,
                        last_vote TEXT NOT NULL
                    )
                """);
            }
        } catch (SQLException e) {
            logger.severe("[CelestialVoting] Failed to initialize vote streak database: " + e.getMessage());
        }
    }

    public static void recordVote(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDate today = LocalDate.now();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT streak, last_vote FROM vote_streaks WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            int streak = 1;
            boolean exists = rs.next();

            if (exists) {
                LocalDate lastVote = LocalDate.parse(rs.getString("last_vote"));
                if (lastVote.equals(today)) return; // already voted today
                if (lastVote.plusDays(1).equals(today)) {
                    streak = rs.getInt("streak") + 1;
                }
            }

            saveStreak(uuid, streak, today);
            int target = votingConfig.getStreakDays();

            if (streak >= target) {
                applyStreakReward(player);
                saveStreak(uuid, 0, today); // reset after reward
            }

        } catch (SQLException e) {
            logger.severe("[CelestialVoting] Error recording vote for " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void saveStreak(UUID uuid, int streak, LocalDate date) {
        try {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO vote_streaks (uuid, streak, last_vote)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET streak = excluded.streak, last_vote = excluded.last_vote
            """);
            ps.setString(1, uuid.toString());
            ps.setInt(2, streak);
            ps.setString(3, date.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[CelestialVoting] Failed to save vote streak for UUID " + uuid + ": " + e.getMessage());
        }
    }

    private static void applyStreakReward(Player player) {
        try {
            var reward = votingConfig.getStreakReward();
            String type = reward.getOrDefault("type", "COMMAND").toString().toUpperCase();
            String broadcastKey = (String) reward.get("broadcast-key");

            switch (type) {
                case "COMMAND" -> {
                    String cmd = reward.get("value").toString().replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                case "ITEM" -> {
                    // Optional: item streak support
                }
            }

            if (broadcastKey != null) {
                Bukkit.broadcast(MessageConfig.mm(broadcastKey)
                        .replaceText(builder -> builder.matchLiteral("%player%").replacement(player.getName())));
            }

        } catch (Exception e) {
            logger.severe("[CelestialVoting] Failed to apply streak reward to " + player.getName() + ": " + e.getMessage());
        }
    }

    public static int getCurrentStreak(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT streak FROM vote_streaks WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("streak");
        } catch (SQLException e) {
            logger.warning("[CelestialVoting] Failed to get streak for UUID " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    public static void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.severe("[CelestialVoting] Failed to close streak database: " + e.getMessage());
        }
    }
}
