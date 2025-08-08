package org.celestialcraft.cCUtilities.modules.activity;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class CelestialPointManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public CelestialPointManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "activity.db");
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().severe("Failed to create plugin data folder.");
                return;
            }
            if (!dbFile.exists() && !dbFile.createNewFile()) {
                plugin.getLogger().severe("Failed to create activity.db file.");
                return;
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS points (" +
                            "uuid TEXT PRIMARY KEY," +
                            "points INTEGER NOT NULL," +
                            "last_active_reward INTEGER," +
                            "last_idle_reward INTEGER)"
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public int getPoints(OfflinePlayer player) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT points FROM points WHERE uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("points") : 0;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting points: " + e.getMessage());
            return 0;
        }
    }

    public int addPoints(OfflinePlayer player, int amount) {
        int current = getPoints(player);
        int newAmount = current + amount;

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO points (uuid, points) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET points = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setInt(2, newAmount);
            stmt.setInt(3, newAmount);
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding points: " + e.getMessage());
        }

        plugin.getLogger().info("[ActivityReward] Added " + amount + " points to " + player.getName() + ". New balance: " + newAmount);
        return newAmount;
    }

    public int removePoints(OfflinePlayer player, int amount) {
        int current = getPoints(player);
        int newAmount = Math.max(0, current - amount);

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO points (uuid, points) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET points = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setInt(2, newAmount);
            stmt.setInt(3, newAmount);
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing points: " + e.getMessage());
        }

        plugin.getLogger().info("[ActivityReward] Removed " + amount + " points from " + player.getName() + ". New balance: " + newAmount);
        return newAmount;
    }

    public void setPoints(OfflinePlayer player, int amount) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO points (uuid, points) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET points = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setInt(2, amount);
            stmt.setInt(3, amount);
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting points: " + e.getMessage());
        }

        plugin.getLogger().info("[ActivityReward] Set " + player.getName() + "'s points to " + amount + ".");
    }

    public void setLastReward(UUID uuid, boolean active, long timestamp) {
        String column = active ? "last_active_reward" : "last_idle_reward";
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO points (uuid, points, " + column + ") VALUES (?, 0, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET " + column + " = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, timestamp);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting last reward: " + e.getMessage());
        }
    }

    public long getLastReward(UUID uuid, boolean active) {
        String column = active ? "last_active_reward" : "last_idle_reward";
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT " + column + " FROM points WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting last reward: " + e.getMessage());
            return 0L;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }

    public void reload() {
        // No-op for now; add any logic as needed later
    }
}
