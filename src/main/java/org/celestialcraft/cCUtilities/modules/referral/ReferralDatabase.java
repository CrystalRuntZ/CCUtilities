package org.celestialcraft.cCUtilities.modules.referral;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ReferralDatabase {
    private Connection conn;
    private final JavaPlugin plugin;

    public ReferralDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "referrals.db");

        try {
            if (!dbFile.exists()) {
                boolean fileCreated = dbFile.createNewFile();
                if (!fileCreated && !dbFile.exists()) {
                    throw new IOException("Failed to create referrals.db file.");
                }
            }

            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTable();
        } catch (IOException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize ReferralDatabase: " + e.getMessage());
        }
    }

    private void createTable() {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS referrals (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "referred_uuid TEXT UNIQUE, " +
                            "referred_name TEXT, " +
                            "referrer_name TEXT, " +
                            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ");"
            );
        } catch (SQLException e) {
            plugin.getLogger().warning("Error creating referrals table: " + e.getMessage());
        }
    }

    public boolean submitReferral(UUID referredUUID, String referredName, String referrerName) {
        String sql = "INSERT INTO referrals (referred_uuid, referred_name, referrer_name) VALUES (?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, referredUUID.toString());
            stmt.setString(2, referredName);
            stmt.setString(3, referrerName);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to submit referral: " + e.getMessage());
            return false;
        }
    }

    public boolean hasReferred(UUID uuid) {
        String sql = "SELECT 1 FROM referrals WHERE referred_uuid = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking if referred: " + e.getMessage());
            return false;
        }
    }

    public String getReferrer(UUID uuid) {
        String sql = "SELECT referrer_name FROM referrals WHERE referred_uuid = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("referrer_name") : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error fetching referrer: " + e.getMessage());
            return null;
        }
    }

    public List<String> getReferredPlayers(String referrer) {
        String sql = "SELECT referred_name FROM referrals WHERE referrer_name = ?;";
        List<String> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, referrer);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("referred_name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error fetching referred players: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> getTopReferrers(int limit) {
        String sql = "SELECT referrer_name, COUNT(*) as count " +
                "FROM referrals " +
                "GROUP BY referrer_name " +
                "ORDER BY count DESC " +
                "LIMIT ?;";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("referrer_name"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error fetching top referrers: " + e.getMessage());
        }
        return result;
    }

    public int getReferralCount(UUID uuid) {
        String sql = "SELECT referrer_name FROM referrals WHERE referred_uuid = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String referrer = rs.getString("referrer_name");
                if (referrer == null) return 0;

                String countSql = "SELECT COUNT(*) as count FROM referrals WHERE referrer_name = ?;";
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    countStmt.setString(1, referrer);
                    ResultSet countRs = countStmt.executeQuery();
                    if (countRs.next()) {
                        return countRs.getInt("count");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error getting referral count: " + e.getMessage());
        }
        return 0;
    }

    public Map<UUID, Integer> getAllReferralCounts() {
        Map<UUID, Integer> result = new HashMap<>();
        String sql = "SELECT referrer_name, COUNT(*) as count FROM referrals GROUP BY referrer_name;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("referrer_name");
                int count = rs.getInt("count");
                OfflinePlayer player = Bukkit.getOfflinePlayer(name);
                if (player.hasPlayedBefore()) {
                    result.put(player.getUniqueId(), count);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading all referral counts: " + e.getMessage());
        }
        return result;
    }
}
