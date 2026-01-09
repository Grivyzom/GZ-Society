package gc.grivyzom.gZSociety.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gc.grivyzom.gZSociety.objects.SocialPlayer;
import org.spongepowered.configurate.ConfigurationNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SQLStorage implements Storage {

    private final HikariDataSource dataSource;

    public SQLStorage(ConfigurationNode dbConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + dbConfig.node("host").getString() + ":" + dbConfig.node("port").getInt()
                + "/" + dbConfig.node("database").getString());
        config.setUsername(dbConfig.node("username").getString());
        config.setPassword(dbConfig.node("password").getString());

        ConfigurationNode poolSettings = dbConfig.node("pool-settings");
        config.setMaximumPoolSize(poolSettings.node("max-pool-size").getInt(10));
        config.setMinimumIdle(poolSettings.node("min-idle").getInt(10));
        config.setMaxLifetime(poolSettings.node("max-lifetime").getInt(1800000));
        config.setConnectionTimeout(poolSettings.node("connection-timeout").getInt(5000));

        // Recommended MySQL settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    public void initDatabase() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // Player data table
            stmt.execute("CREATE TABLE IF NOT EXISTS gzs_players ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "username VARCHAR(16) NOT NULL,"
                    + "notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE"
                    + ");");

            // Friends table (many-to-many relationship)
            stmt.execute("CREATE TABLE IF NOT EXISTS gzs_friends ("
                    + "player_uuid VARCHAR(36) NOT NULL,"
                    + "friend_uuid VARCHAR(36) NOT NULL,"
                    + "is_best_friend BOOLEAN NOT NULL DEFAULT FALSE,"
                    + "PRIMARY KEY (player_uuid, friend_uuid),"
                    + "FOREIGN KEY (player_uuid) REFERENCES gzs_players(uuid) ON DELETE CASCADE,"
                    + "FOREIGN KEY (friend_uuid) REFERENCES gzs_players(uuid) ON DELETE CASCADE"
                    + ");");

            // Friend requests table
            stmt.execute("CREATE TABLE IF NOT EXISTS gzs_friend_requests ("
                    + "sender_uuid VARCHAR(36) NOT NULL,"
                    + "receiver_uuid VARCHAR(36) NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (sender_uuid, receiver_uuid),"
                    + "FOREIGN KEY (sender_uuid) REFERENCES gzs_players(uuid) ON DELETE CASCADE,"
                    + "FOREIGN KEY (receiver_uuid) REFERENCES gzs_players(uuid) ON DELETE CASCADE"
                    + ");");

            // TODO: Add tables for ignored and blocked players
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<SocialPlayer> loadPlayer(UUID playerId, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Ensure player exists in the main table (UPSERT)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO gzs_players (uuid, username, notifications_enabled) VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE username = ?")) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, playerName);
                    ps.executeUpdate();
                }

                SocialPlayer socialPlayer = new SocialPlayer(playerId, playerName);

                // Load notification preference
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT notifications_enabled FROM gzs_players WHERE uuid = ?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            socialPlayer.setNotificationsEnabled(rs.getBoolean("notifications_enabled"));
                        }
                    }
                }

                // Load friends
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT friend_uuid, is_best_friend FROM gzs_friends WHERE player_uuid = ?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID friendId = UUID.fromString(rs.getString("friend_uuid"));
                            socialPlayer.addFriend(friendId);
                            if (rs.getBoolean("is_best_friend")) {
                                socialPlayer.addBestFriend(friendId);
                            }
                        }
                    }
                }

                // Load outgoing friend requests (I sent these)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT receiver_uuid FROM gzs_friend_requests WHERE sender_uuid = ?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID receiverId = UUID.fromString(rs.getString("receiver_uuid"));
                            socialPlayer.sendRequest(receiverId);
                        }
                    }
                }

                // Load incoming friend requests (received from others)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sender_uuid FROM gzs_friend_requests WHERE receiver_uuid = ?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID senderId = UUID.fromString(rs.getString("sender_uuid"));
                            socialPlayer.receiveRequest(senderId);
                        }
                    }
                }

                // TODO: Load ignored and blocked players
                return socialPlayer;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load player data for " + playerName, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayer(SocialPlayer player) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String playerUuid = player.getPlayerId().toString();

                // Update notification preference
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE gzs_players SET notifications_enabled = ? WHERE uuid = ?")) {
                    ps.setBoolean(1, player.isNotificationsEnabled());
                    ps.setString(2, playerUuid);
                    ps.executeUpdate();
                }

                // Clear existing friend relationships
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM gzs_friends WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid);
                    ps.executeUpdate();
                }

                // Save friends
                if (!player.getFriends().isEmpty()) {
                    String friendsSql = "INSERT INTO gzs_friends (player_uuid, friend_uuid, is_best_friend) VALUES "
                            + player.getFriends().stream()
                                    .map(friendId -> "(?, ?, ?)")
                                    .collect(Collectors.joining(", "));

                    try (PreparedStatement ps = conn.prepareStatement(friendsSql)) {
                        int i = 1;
                        for (UUID friendId : player.getFriends()) {
                            ps.setString(i++, playerUuid);
                            ps.setString(i++, friendId.toString());
                            ps.setBoolean(i++, player.getBestFriends().contains(friendId));
                        }
                        ps.executeUpdate();
                    }
                }

                // Clear existing outgoing friend requests from this player
                try (PreparedStatement ps = conn
                        .prepareStatement("DELETE FROM gzs_friend_requests WHERE sender_uuid = ?")) {
                    ps.setString(1, playerUuid);
                    ps.executeUpdate();
                }

                // Save outgoing friend requests
                if (!player.getOutgoingRequests().isEmpty()) {
                    String requestsSql = "INSERT INTO gzs_friend_requests (sender_uuid, receiver_uuid) VALUES "
                            + player.getOutgoingRequests().stream()
                                    .map(receiverId -> "(?, ?)")
                                    .collect(Collectors.joining(", "));

                    try (PreparedStatement ps = conn.prepareStatement(requestsSql)) {
                        int i = 1;
                        for (UUID receiverId : player.getOutgoingRequests()) {
                            ps.setString(i++, playerUuid);
                            ps.setString(i++, receiverId.toString());
                        }
                        ps.executeUpdate();
                    }
                }

                // TODO: Save ignored and blocked players
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to save player data for " + player.getPlayerName(), e);
            }
        });
    }
}
