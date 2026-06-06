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
import java.util.Set;
import java.util.stream.Collectors;

public class NChatCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public NChatCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "op" -> handleOp(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6=== NekoChat 聊天管理系统 ==="));
        sender.sendMessage(plugin.colorize("&e/ptitle add <玩家> <头衔> &7- 设置玩家头衔"));
        sender.sendMessage(plugin.colorize("&e/ptitle remove <玩家> &7- 移除玩家头衔"));
        sender.sendMessage(plugin.colorize("&e/cban add/remove/list &7- 管理屏蔽词"));
        sender.sendMessage(plugin.colorize("&e/cwhitelist add/remove/list &7- 管理宣传白名单"));
        sender.sendMessage(plugin.colorize("&e/mute <玩家> [时间] &7- 禁言玩家"));
        sender.sendMessage(plugin.colorize("&e/unmute <玩家> &7- 解除禁言"));
        sender.sendMessage(plugin.colorize("&e/nchat &7- 显示本帮助"));
        sender.sendMessage(plugin.colorize("&e/nchat reload &7- 重新加载配置文件"));
        sender.sendMessage(plugin.colorize("&e/nchat op add <玩家> &7- 添加管理员"));
        sender.sendMessage(plugin.colorize("&e/nchat op remove <玩家> &7- 移除管理员"));
        sender.sendMessage(plugin.colorize("&e/nchat op list &7- 查看管理员列表"));
        sender.sendMessage(plugin.colorize("&8GitHub: &7https://github.com/ZhouyiStudio/NekoChat"));
        sender.sendMessage(plugin.colorize("&8Author: &7Zhouyi"));
    }

    private void handleReload(CommandSender sender) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.colorize("&c你没有权限执行此命令！"));
            return;
        }

        plugin.reloadConfig();
        plugin.getCBanManager().reload();
        plugin.getDatabaseManager().reload();

        sender.sendMessage(plugin.colorize("&a配置文件已重新加载！"));
        plugin.getLogger().info(sender.getName() + " 执行了 /nchat reload");
    }

    private void handleOp(CommandSender sender, String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.colorize("&c你没有权限执行此命令！"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&c用法: /nchat op add <玩家> | /nchat op remove <玩家> | /nchat op list"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.colorize("&c用法: /nchat op add <玩家>"));
                    return;
                }
                String targetName = args[2];
                plugin.getOpManager().add(targetName);
                sender.sendMessage(plugin.colorize("&a已将 &e" + targetName + " &a添加为 NekoChat 管理员！"));
                plugin.getLogger().info(sender.getName() + " 添加了管理员: " + targetName);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.colorize("&c用法: /nchat op remove <玩家>"));
                    return;
                }
                String targetName = args[2];
                if (!plugin.getOpManager().isOp(targetName)) {
                    sender.sendMessage(plugin.colorize("&e" + targetName + " &c当前不是 NekoChat 管理员。"));
                    return;
                }
                plugin.getOpManager().remove(targetName);
                sender.sendMessage(plugin.colorize("&a已移除 &e" + targetName + " &a的 NekoChat 管理员权限！"));
                plugin.getLogger().info(sender.getName() + " 移除了管理员: " + targetName);
            }
            case "list" -> {
                Set<String> ops = plugin.getOpManager().getOps();
                if (ops.isEmpty()) {
                    sender.sendMessage(plugin.colorize("&e当前没有 NekoChat 管理员。"));
                } else {
                    sender.sendMessage(plugin.colorize("&6=== NekoChat 管理员列表 (" + ops.size() + "个) ==="));
                    for (String name : ops) {
                        sender.sendMessage(plugin.colorize("&7- &e" + name));
                    }
                }
            }
            default -> {
                sender.sendMessage(plugin.colorize("&c未知子命令！可用: add, remove, list"));
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("reload", "op"));
            // 只给管理员显示 op 子命令的 tab 提示
            if (!plugin.isAdmin(sender)) {
                completions.remove("op");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("op")) {
            if (!plugin.isAdmin(sender)) return List.of();
            return List.of("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("op")
                && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("del"))) {
            return plugin.getOpManager().getOps().stream()
                    .filter(n -> n.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("op")
                && (args[1].equalsIgnoreCase("add"))) {
            // 提示在线玩家
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
