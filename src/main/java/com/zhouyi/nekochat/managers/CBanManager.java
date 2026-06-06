package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CBanManager {

    private final NekoChat plugin;
    private final List<String> bannedWords = new ArrayList<>();
    private final List<String> whitelistWords = new ArrayList<>();
    private File dataFile;
    private FileConfiguration data;

    // IP 地址正则
    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );
    // 域名正则 (匹配常见顶级域名)
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "\\b[a-zA-Z0-9.-]+\\.(?:com|net|org|me|io|xyz|top|cc|gg|fun|club|live|site|host|pro|info|online|world|vip|win|bid|cn|tk|ml|ga)\\b"
    );

    public CBanManager(NekoChat plugin) {
        this.plugin = plugin;
        load();
    }

    // ====== 屏蔽词 ======

    public void addWord(String word) {
        String lower = word.toLowerCase();
        if (!bannedWords.contains(lower)) {
            bannedWords.add(lower);
            save();
        }
    }

    public void removeWord(String word) {
        bannedWords.remove(word.toLowerCase());
        save();
    }

    public boolean containsBannedWord(String message) {
        String lower = message.toLowerCase();
        for (String word : bannedWords) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getBannedWords() {
        return new ArrayList<>(bannedWords);
    }

    // ====== 宣传检测白名单 ======

    /**
     * 添加白名单词汇（域名或关键词，包含即跳过检测）
     */
    public void addWhitelistWord(String word) {
        String lower = word.toLowerCase();
        if (!whitelistWords.contains(lower)) {
            whitelistWords.add(lower);
            save();
        }
    }

    /**
     * 移除白名单词汇
     */
    public void removeWhitelistWord(String word) {
        whitelistWords.remove(word.toLowerCase());
        save();
    }

    /**
     * 获取所有白名单词汇
     */
    public List<String> getWhitelistWords() {
        return new ArrayList<>(whitelistWords);
    }

    /**
     * 检查消息是否被白名单放行
     */
    private boolean isWhitelisted(String lowerMessage) {
        for (String w : whitelistWords) {
            if (lowerMessage.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查消息是否包含服务器宣传
     * @return 匹配到的非法服务器地址，null 表示通过
     */
    public String containsServerAd(String message) {
        if (!plugin.getConfig().getBoolean("server-ad-check", true)) {
            return null;
        }

        String lower = message.toLowerCase();

        // 白名单放行
        if (isWhitelisted(lower)) {
            return null;
        }

        // 检测 IP 地址
        var ipMatcher = IP_PATTERN.matcher(lower);
        if (ipMatcher.find()) {
            return ipMatcher.group();
        }

        // 检测域名
        var domainMatcher = DOMAIN_PATTERN.matcher(lower);
        if (domainMatcher.find()) {
            return domainMatcher.group();
        }

        return null;
    }

    // ====== 数据持久化 ======

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "cban.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 cban.yml: " + e.getMessage());
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // 从配置文件加载默认屏蔽词
        List<String> defaultBanned = plugin.getConfig().getStringList("banned-words");
        for (String w : defaultBanned) {
            String lower = w.toLowerCase();
            if (!bannedWords.contains(lower)) {
                bannedWords.add(lower);
            }
        }

        // 从数据文件加载自定义屏蔽词
        List<String> savedWords = data.getStringList("words");
        for (String w : savedWords) {
            String lower = w.toLowerCase();
            if (!bannedWords.contains(lower)) {
                bannedWords.add(lower);
            }
        }

        // 从配置文件加载默认白名单
        List<String> defaultWhitelist = plugin.getConfig().getStringList("server-ad-whitelist");
        for (String w : defaultWhitelist) {
            String lower = w.toLowerCase();
            if (!whitelistWords.contains(lower)) {
                whitelistWords.add(lower);
            }
        }

        // 从数据文件加载自定义白名单
        List<String> savedWhitelist = data.getStringList("whitelist");
        for (String w : savedWhitelist) {
            String lower = w.toLowerCase();
            if (!whitelistWords.contains(lower)) {
                whitelistWords.add(lower);
            }
        }
    }

    public void save() {
        if (data == null) return;
        data.set("words", bannedWords);
        data.set("whitelist", whitelistWords);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 cban.yml: " + e.getMessage());
        }
    }
}
