package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PTitleCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public PTitleCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.msg("ptitle-help-add"));
            sender.sendMessage(plugin.msg("ptitle-help-remove"));
            sender.sendMessage(plugin.colorize("&7示例: /ptitle add Steve &b&l大神"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("ptitle-usage-add"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.colorize("&c玩家 " + args[1] + " 不在线！"));
                    return true;
                }

                // 拼接头衔
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String title = sb.toString();

                // 限制头衔长度
                if (title.replace("&", "").replaceAll("[0-9a-fklmnor]", "").length() > 32) {
                    sender.sendMessage(plugin.colorize("&c头衔过长！最大支持32个字符（不含颜色代码）。"));
                    return true;
                }

                plugin.getTitleManager().setTitle(target, title);
                target.playerListName(plugin.getTitleManager().getTabName(target));
                sender.sendMessage(plugin.msg("ptitle-added", target.getName(), title));
                target.sendMessage(plugin.colorize("&a你的头衔已被设置为: " + title));
            }
            case "remove", "del", "delete", "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("ptitle-usage-remove"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.colorize("&c玩家 " + args[1] + " 不在线！"));
                    return true;
                }

                plugin.getTitleManager().removeTitle(target);
                target.playerListName(target.name());
                sender.sendMessage(plugin.msg("ptitle-removed", target.getName()));
                target.sendMessage(plugin.colorize("&a你的头衔已被移除！"));
            }
            default -> {
                sender.sendMessage(plugin.colorize("&c未知子命令！可用: add, remove"));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove");
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
