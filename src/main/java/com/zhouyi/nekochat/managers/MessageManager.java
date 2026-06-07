package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * 消息管理器 — 从 messages.yml 加载所有可自定义消息
 */
public class MessageManager {

    private final NekoChat plugin;
    private FileConfiguration messagesConfig;

    public MessageManager(NekoChat plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加载 messages.yml
     */
    public void reload() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // 如果文件不存在，从 jar 中复制默认
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 合并 jar 内的默认值（新增 key 自动补全）
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
            // 使用新配置（含有默认值）
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    /**
     * 获取原始消息文本（含 & 颜色代码）
     *
     * @param key 消息键名
     * @return 消息文本，不存在则返回 "&c缺失消息: {key}"
     */
    public String getRaw(String key) {
        String msg = messagesConfig.getString(key);
        if (msg == null) {
            // 尝试从默认值获取
            if (messagesConfig.getDefaults() != null) {
                msg = messagesConfig.getDefaults().getString(key);
            }
        }
        return (msg != null) ? msg : "&c缺失消息: " + key;
    }

    /**
     * 获取格式化后的消息文本（替换 {0} {1} ... 占位符）
     *
     * @param key  消息键名
     * @param args 占位符参数
     * @return 格式化后的消息文本
     */
    public String format(String key, String... args) {
        String msg = getRaw(key);
        if (args.length == 0) return msg;
        // 替换 {0} {1} {2} ...
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", args[i] != null ? args[i] : "");
        }
        return msg;
    }

    /**
     * 获取已解析颜色代码的 Adventure Component
     */
    public Component get(String key, String... args) {
        return plugin.colorize(format(key, args));
    }
}
