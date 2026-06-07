package com.zhouyi.nekochat;

import com.zhouyi.nekochat.commands.AICommand;
import com.zhouyi.nekochat.commands.CBanCommand;
import com.zhouyi.nekochat.commands.CWhitelistCommand;
import com.zhouyi.nekochat.commands.MuteCommand;
import com.zhouyi.nekochat.commands.NChatCommand;
import com.zhouyi.nekochat.commands.PTitleCommand;
import com.zhouyi.nekochat.commands.UnmuteCommand;
import com.zhouyi.nekochat.listeners.ChatListener;
import com.zhouyi.nekochat.listeners.ServerMessageListener;
import com.zhouyi.nekochat.managers.AIManager;
import com.zhouyi.nekochat.managers.CBanManager;
import com.zhouyi.nekochat.managers.DatabaseManager;
import com.zhouyi.nekochat.managers.MuteManager;
import com.zhouyi.nekochat.managers.OpManager;
import com.zhouyi.nekochat.managers.TitleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NekoChat extends JavaPlugin {

    private static NekoChat instance;
    private TitleManager titleManager;
    private CBanManager cbanManager;
    private MuteManager muteManager;
    private DatabaseManager databaseManager;
    private OpManager opManager;
    private AIManager aiManager;
    private MiniMessage miniMessage;

    // URL 正则（公开给其他类使用）
    static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w./?=&#%+~@,:;!-]+|www\\.[\\w./?=&#%+~@,:;!-]+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();

        // 保存默认配置
        saveDefaultConfig();

        // 确保旧配置中包含 AI 配置段
        ensureConfigSections();

        // 初始化管理器
        this.titleManager = new TitleManager(this);
        this.cbanManager = new CBanManager(this);
        this.muteManager = new MuteManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.opManager = new OpManager(this);
        this.aiManager = new AIManager(this);

        // 注册命令
        var ptitleCmd = getCommand("ptitle");
        if (ptitleCmd != null) {
            ptitleCmd.setExecutor(new PTitleCommand(this));
            ptitleCmd.setTabCompleter(new PTitleCommand(this));
        }

        var cbanCmd = getCommand("cban");
        if (cbanCmd != null) {
            cbanCmd.setExecutor(new CBanCommand(this));
            cbanCmd.setTabCompleter(new CBanCommand(this));
        }

        var muteCmd = getCommand("mute");
        if (muteCmd != null) {
            muteCmd.setExecutor(new MuteCommand(this));
            muteCmd.setTabCompleter(new MuteCommand(this));
        }

        var unmuteCmd = getCommand("unmute");
        if (unmuteCmd != null) {
            unmuteCmd.setExecutor(new UnmuteCommand(this));
            unmuteCmd.setTabCompleter(new UnmuteCommand(this));
        }

        var cwhitelistCmd = getCommand("cwhitelist");
        if (cwhitelistCmd != null) {
            cwhitelistCmd.setExecutor(new CWhitelistCommand(this));
            cwhitelistCmd.setTabCompleter(new CWhitelistCommand(this));
        }

        var nchatCmd = getCommand("nchat");
        if (nchatCmd != null) {
            nchatCmd.setExecutor(new NChatCommand(this));
            nchatCmd.setTabCompleter(new NChatCommand(this));
        }

        var aiCmd = getCommand("ai");
        if (aiCmd != null) {
            aiCmd.setExecutor(new AICommand(this));
            aiCmd.setTabCompleter(new AICommand(this));
        }

        // 注册事件监听
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerMessageListener(this), this);

        getLogger().info("============================================================");
        getLogger().info("              NekoChat 聊天管理系统 已启用!");
        getLogger().info("      作者: Zhouyi | GitHub: https://github.com/ZhouyiStudio/NekoChat");
        getLogger().info("============================================================");
    }

    @Override
    public void onDisable() {
        // 保存数据
        if (titleManager != null) titleManager.save();
        if (cbanManager != null) cbanManager.save();
        if (muteManager != null) muteManager.save();
        if (databaseManager != null) databaseManager.shutdown();
        getLogger().info("============================================================");
        getLogger().info("                    NekoChat 已禁用！");
        getLogger().info("============================================================");
    }

    /**
     * 确保旧版本的 config.yml 包含 AI 配置段
     * saveDefaultConfig() 只会在文件不存在时复制，已有旧配置不会自动新增
     */
    private void ensureConfigSections() {
        var config = getConfig();
        boolean changed = false;

        if (!config.contains("ai")) {
            config.set("ai.enabled", false);
            config.set("ai.api-url", "https://api-inference.modelscope.cn/v1/chat/completions");
            config.set("ai.api-key", "");
            config.set("ai.model", "Qwen/Qwen2.5-7B-Instruct");
            config.set("ai.system-prompt", "你是 NekoChat AI，一个 Minecraft 服务器中的智能助手。请用中文回复，回答简洁友好。每次回答不要太长。");
            config.set("ai.max-tokens", 512);
            config.set("ai.timeout-seconds", 30);
            config.set("ai.cooldown-seconds", 20);
            config.set("ai.public-reply", true);
            config.set("ai.display-name", "&b[AI助手]");
            config.set("ai.prompts.翻译", "你是一个翻译助手，请将用户的输入翻译成英文，只输出翻译结果。");
            config.set("ai.prompts.笑话", "你是一个讲笑话的 AI，用中文讲一个简短有趣的笑话。");
            config.set("ai.prompts.代码", "你是编程专家，用中文解释代码问题，给出简洁的代码示例。");
            changed = true;
        } else {
            // 已有 ai 段，检查是否有 prompts 子段
            if (!config.contains("ai.prompts")) {
                config.set("ai.prompts.翻译", "你是一个翻译助手，请将用户的输入翻译成英文，只输出翻译结果。");
                config.set("ai.prompts.笑话", "你是一个讲笑话的 AI，用中文讲一个简短有趣的笑话。");
                config.set("ai.prompts.代码", "你是编程专家，用中文解释代码问题，给出简洁的代码示例。");
                changed = true;
            }
        }

        if (changed) {
            saveConfig();
            getLogger().info("§a[NekoChat] 已自动添加 AI 配置段到 config.yml");
        }
    }

    public static NekoChat getInstance() {
        return instance;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public CBanManager getCBanManager() {
        return cbanManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public OpManager getOpManager() {
        return opManager;
    }

    public AIManager getAIManager() {
        return aiManager;
    }

    /**
     * 检查 CommandSender 是否有管理员权限
     * 只有控制台和通过 /nchat op add 添加的玩家有权限
     * Bukkit OP 玩家默认没有权限，需要单独添加
     */
    public boolean isAdmin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        if (sender instanceof Player) {
            return opManager.isOp(sender.getName());
        }
        return false;
    }

    /**
     * 是否启用调试日志
     */
    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    /**
     * 将带 & 的颜色代码转换为 Adventure Component
     */
    public Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * 处理消息: 解析 & 颜色代码 + URL 转可点击链接
     * 非 URL 部分保留颜色代码，URL 部分显示为天蓝色可点击
     */
    public Component processMessage(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            // 没有 URL，直接解析颜色代码
            return colorize(text);
        }

        Component result = Component.empty();
        int lastEnd = 0;
        matcher.reset();

        while (matcher.find()) {
            // URL 前面的文本（含颜色代码）
            if (matcher.start() > lastEnd) {
                result = result.append(colorize(text.substring(lastEnd, matcher.start())));
            }

            String url = matcher.group();
            String clickUrl = url.startsWith("http") ? url : "https://" + url;

            Component urlComponent = Component.text(url)
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(clickUrl))
                    .hoverEvent(colorize("&b点击打开: " + url));

            result = result.append(urlComponent);
            lastEnd = matcher.end();
        }

        // 剩余文本
        if (lastEnd < text.length()) {
            result = result.append(colorize(text.substring(lastEnd)));
        }

        return result;
    }

    /**
     * 将 MiniMessage 格式字符串转换为 Component
     */
    public Component miniMessage(String text) {
        return miniMessage.deserialize(text);
    }
}
