package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteManager {

    private final NekoChat plugin;
    // UUID -> 解禁时间戳 (毫秒), -1 表示永久禁言
    private final Map<UUID, Long> mutedPlayers = new HashMap<>();
    private File dataFile;
    private FileConfiguration data;

    public MuteManager(NekoChat plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 禁言玩家
     * @param player 目标玩家
     * @param duration 持续时间（秒），-1 表示永久
     */
    public void mute(Player player, long duration) {
        long expiry = (duration == -1) ? -1 : System.currentTimeMillis() + (duration * 1000);
        mutedPlayers.put(player.getUniqueId(), expiry);
        save();
    }

    /**
     * 解除禁言
     */
    public void unmute(Player player) {
        mutedPlayers.remove(player.getUniqueId());
        save();
    }

    /**
     * 检查玩家是否被禁言
     */
    public boolean isMuted(Player player) {
        Long expiry = mutedPlayers.get(player.getUniqueId());
        if (expiry == null) return false;
        if (expiry == -1) return true; // 永久禁言
        if (System.currentTimeMillis() > expiry) {
            // 已过期，自动解除
            mutedPlayers.remove(player.getUniqueId());
            save();
            return false;
        }
        return true;
    }

    /**
     * 获取禁言剩余时间文本
     */
    public String getRemainingTime(Player player) {
        Long expiry = mutedPlayers.get(player.getUniqueId());
        if (expiry == null) return null;
        if (expiry == -1) return "永久";

        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            mutedPlayers.remove(player.getUniqueId());
            save();
            return null;
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "天" + (hours % 24) + "小时" + (minutes % 60) + "分";
        if (hours > 0) return hours + "小时" + (minutes % 60) + "分" + (seconds % 60) + "秒";
        if (minutes > 0) return minutes + "分" + (seconds % 60) + "秒";
        return seconds + "秒";
    }

    /**
     * 检测玩家是否被禁言，发送提示
     */
    public boolean checkAndNotify(Player player) {
        if (!isMuted(player)) return false;

        String remaining = getRemainingTime(player);
        String msg = plugin.getConfig().getString("mute-message", "&c你已被禁言{reason}&c！")
                .replace("{reason}", remaining != null ? "，剩余: " + remaining : "");

        // 使用 & 颜色代码
        player.sendMessage(plugin.colorize(msg));
        return true;
    }

    /**
     * 从文件加载数据
     */
    private void load() {
        dataFile = new File(plugin.getDataFolder(), "mute.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 mute.yml: " + e.getMessage());
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long expiry = data.getLong(key);
                mutedPlayers.put(uuid, expiry);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的UUID: " + key);
            }
        }
    }

    /**
     * 保存数据到文件
     */
    public void save() {
        if (data == null) return;
        for (Map.Entry<UUID, Long> entry : mutedPlayers.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 mute.yml: " + e.getMessage());
        }
    }
}
