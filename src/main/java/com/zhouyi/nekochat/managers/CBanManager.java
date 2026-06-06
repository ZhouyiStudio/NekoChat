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
    // 允许的服务器
    private static final String ALLOWED_SERVER = "3d3k.org";

    public CBanManager(NekoChat plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 添加屏蔽词
     */
    public void addWord(String word) {
        String lower = word.toLowerCase();
        if (!bannedWords.contains(lower)) {
            bannedWords.add(lower);
            save();
        }
    }

    /**
     * 移除屏蔽词
     */
    public void removeWord(String word) {
        bannedWords.remove(word.toLowerCase());
        save();
    }

    /**
     * 检查消息是否包含屏蔽词
     */
    public boolean containsBannedWord(String message) {
        String lower = message.toLowerCase();
        for (String word : bannedWords) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查消息是否包含服务器宣传 (只允许 *.3d3k.org)
     * @return 匹配到的非法服务器地址，null 表示通过
     */
    public String containsServerAd(String message) {
        // 配置中关闭了检测
        if (!plugin.getConfig().getBoolean("server-ad-check", true)) {
            return null;
        }

        String lower = message.toLowerCase();

        // 如果包含允许的服务器，跳过检测
        if (lower.contains(ALLOWED_SERVER)) {
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

    /**
     * 获取所有屏蔽词列表
     */
    public List<String> getBannedWords() {
        return new ArrayList<>(bannedWords);
    }

    /**
     * 从文件加载数据（合并默认屏蔽词）
     */
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
        List<String> defaultWords = plugin.getConfig().getStringList("banned-words");
        for (String w : defaultWords) {
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
    }

    /**
     * 保存数据到文件
     */
    public void save() {
        if (data == null) return;
        data.set("words", bannedWords);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 cban.yml: " + e.getMessage());
        }
    }
}
