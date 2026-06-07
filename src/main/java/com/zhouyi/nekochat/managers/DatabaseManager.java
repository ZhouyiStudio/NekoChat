package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final NekoChat plugin;
    private HikariDataSource dataSource;
    private boolean enabled = false;
    private String tablePrefix;

    // 异步批量插入缓冲区
    private static final String INSERT_SQL =
            "INSERT INTO %schat_logs (player_name, player_uuid, message, type, timestamp) VALUES (?, ?, ?, ?, ?)";

    public DatabaseManager(NekoChat plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        var config = plugin.getConfig().getConfigurationSection("database");
        if (config == null) {
            plugin.getLogger().info("§e[NekoChat] 未配置数据库，聊天记录功能已跳过。");
            return;
        }

        String host = config.getString("host");
        if (host == null || host.isEmpty()) {
            plugin.getLogger().info("§e[NekoChat] 数据库 host 为空，聊天记录功能已跳过。");
            return;
        }

        int port = config.getInt("port", 3306);
        String database = config.getString("database", "nekochat");
        String user = config.getString("user", "root");
        String password = config.getString("password", "");
        tablePrefix = config.getString("table-prefix", "nc_");
        int poolSize = config.getInt("pool-size", 10);
        int timeout = config.getInt("connection-timeout", 5000);

        try {
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&characterEncoding=UTF-8&rewriteBatchedStatements=true");
            hikari.setUsername(user);
            hikari.setPassword(password);
            hikari.setMaximumPoolSize(poolSize);
            hikari.setConnectionTimeout(timeout);
            hikari.setMinimumIdle(2);
            hikari.setPoolName("NekoChat-Pool");

            dataSource = new HikariDataSource(hikari);
            enabled = true;

            // 创建表
            createTables();

            plugin.getLogger().info("§a[NekoChat] MySQL 连接成功！聊天记录将写入 " + host + "/" + database);
        } catch (Exception e) {
            plugin.getLogger().severe("§c[NekoChat] MySQL 连接失败: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * 自动创建聊天记录表
     */
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "chat_logs ("
                + "  id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "  player_name VARCHAR(36) NOT NULL,"
                + "  player_uuid VARCHAR(36) DEFAULT NULL,"
                + "  message TEXT NOT NULL,"
                + "  type VARCHAR(20) NOT NULL DEFAULT 'chat',"
                + "  timestamp BIGINT NOT NULL,"
                + "  INDEX idx_time (timestamp),"
                + "  INDEX idx_player (player_name)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * 记录聊天消息（异步）
     */
    public CompletableFuture<Void> logChat(String playerName, String playerUuid, String message, String type) {
        if (!enabled) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            String sql = String.format(INSERT_SQL, tablePrefix);
            long now = System.currentTimeMillis() / 1000;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerName);
                stmt.setString(2, playerUuid);
                stmt.setString(3, message);
                stmt.setString(4, type);
                stmt.setLong(5, now);
                stmt.executeUpdate();
                if (plugin.isDebug()) {
                    plugin.getLogger().info("§7[NekoChat] 聊天记录已写入: " + playerName + " [" + type + "]");
                }

            } catch (SQLException e) {
                plugin.getLogger().warning("写入聊天记录失败: " + e.getMessage());
            }
        });
    }

    /**
     * 记录玩家聊天
     */
    public CompletableFuture<Void> logPlayerChat(String playerName, String playerUuid, String message) {
        return logChat(playerName, playerUuid, message, "chat");
    }

    /**
     * 记录私聊消息
     *
     * @param playerName   发送者名称
     * @param playerUuid   发送者 UUID
     * @param targetPlayer 接收者名称
     * @param message      消息内容
     */
    public CompletableFuture<Void> logPrivateChat(String playerName, String playerUuid, String targetPlayer, String message) {
        return logChat(playerName, playerUuid, "-> " + targetPlayer + " : " + message, "whisper");
    }

    /**
     * 记录服务器消息
     */
    public CompletableFuture<Void> logServerMessage(String message) {
        return logChat("[Server]", null, message, "system");
    }

    /**
     * 记录 AI 交互记录
     *
     * @param playerName 提问的玩家名称
     * @param question   玩家的问题
     * @param answer     AI 的回答
     */
    public CompletableFuture<Void> logAIInteraction(String playerName, String question, String answer) {
        return logChat(playerName, null, "Q: " + question + " | A: " + answer, "ai");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 重新加载数据库配置（/nchat reload 时调用）
     * 关闭旧连接池，重新读取 config.yml 并初始化新连接。
     */
    public void reload() {
        shutdown();
        enabled = false;
        dataSource = null;
        init();
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接池已关闭。");
        }
    }
}
