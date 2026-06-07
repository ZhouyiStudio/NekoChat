package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public BroadcastCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.colorize("&c用法: /broadcast <消息>"));
            return true;
        }

        String msg = String.join(" ", args);

        // 获取广播前缀
        String prefix = plugin.getConfig().getString("broadcast-prefix", "&6[公告] ");
        Component broadcastMsg = plugin.colorize(prefix + "&r" + msg);

        Bukkit.broadcast(broadcastMsg);

        plugin.getLogger().info("§6[NekoChat]§r " + sender.getName() + " 发送了公告: " + msg);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
