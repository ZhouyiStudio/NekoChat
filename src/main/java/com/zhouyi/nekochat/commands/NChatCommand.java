package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class NChatCommand implements CommandExecutor {

    private final NekoChat plugin;

    public NChatCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // 重载权限检查
            if (!sender.hasPermission("nekochat.nchat")) {
                sender.sendMessage(plugin.colorize("&c你没有权限执行此命令！"));
                return true;
            }

            plugin.reloadConfig();
            plugin.getCBanManager().reload();
            plugin.getDatabaseManager().reload();

            sender.sendMessage(plugin.colorize("&a配置文件已重新加载！"));
            plugin.getLogger().info(sender.getName() + " 执行了 /nchat reload");
            return true;
        }

        sender.sendMessage(plugin.colorize("&6=== NekoChat 聊天管理系统 ==="));
        sender.sendMessage(plugin.colorize("&e/ptitle add <玩家> <头衔> &7- 设置玩家头衔 (OP)"));
        sender.sendMessage(plugin.colorize("&e/ptitle remove <玩家> &7- 移除玩家头衔 (OP)"));
        sender.sendMessage(plugin.colorize("&e/cban add/remove/list &7- 管理屏蔽词 (OP)"));
        sender.sendMessage(plugin.colorize("&e/cwhitelist add/remove/list &7- 管理宣传白名单 (OP)"));
        sender.sendMessage(plugin.colorize("&e/mute <玩家> [时间] &7- 禁言玩家 (OP)"));
        sender.sendMessage(plugin.colorize("&e/unmute <玩家> &7- 解除禁言 (OP)"));
        sender.sendMessage(plugin.colorize("&e/nchat &7- 显示本帮助"));
        sender.sendMessage(plugin.colorize("&e/nchat reload &7- 重新加载配置文件"));
        sender.sendMessage(plugin.colorize("&8GitHub: &7https://github.com/ZhouyiStudio/NekoChat"));
        sender.sendMessage(plugin.colorize("&8Author: &7Zhouyi"));
        return true;
    }
}
