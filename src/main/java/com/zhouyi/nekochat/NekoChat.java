package com.zhouyi.nekochat;

import com.zhouyi.nekochat.commands.CBanCommand;
import com.zhouyi.nekochat.commands.CWhitelistCommand;
import com.zhouyi.nekochat.commands.MuteCommand;
import com.zhouyi.nekochat.commands.NChatCommand;
import com.zhouyi.nekochat.commands.PTitleCommand;
import com.zhouyi.nekochat.commands.UnmuteCommand;
import com.zhouyi.nekochat.listeners.ChatListener;
import com.zhouyi.nekochat.managers.CBanManager;
import com.zhouyi.nekochat.managers.MuteManager;
import com.zhouyi.nekochat.managers.TitleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class NekoChat extends JavaPlugin {

    private static NekoChat instance;
    private TitleManager titleManager;
    private CBanManager cbanManager;
    private MuteManager muteManager;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();

        // 保存默认配置
        saveDefaultConfig();

        // 初始化管理器
        this.titleManager = new TitleManager(this);
        this.cbanManager = new CBanManager(this);
        this.muteManager = new MuteManager(this);

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
        }

        // 注册事件监听
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("NekoChat 聊天管理系统 已启用!");
        getLogger().info("作者: Zhouyi | GitHub: https://github.com/ZhouyiStudio/NekoChat");
    }

    @Override
    public void onDisable() {
        // 保存数据
        if (titleManager != null) titleManager.save();
        if (cbanManager != null) cbanManager.save();
        if (muteManager != null) muteManager.save();
        getLogger().info("NekoChat 已禁用！");
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

    /**
     * 将带 & 的颜色代码转换为 Adventure Component
     */
    public Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * 将 MiniMessage 格式字符串转换为 Component
     */
    public Component miniMessage(String text) {
        return miniMessage.deserialize(text);
    }
}
