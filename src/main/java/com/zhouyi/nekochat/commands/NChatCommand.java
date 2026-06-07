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
            case "reload" -> handleReload(sender, args);
            case "op" -> handleOp(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.msg("nchat-help-header"));
        sender.sendMessage(plugin.msg("nchat-help-ptitle"));
        sender.sendMessage(plugin.msg("nchat-help-cban"));
        sender.sendMessage(plugin.msg("nchat-help-cwhitelist"));
        sender.sendMessage(plugin.msg("nchat-help-mute"));
        sender.sendMessage(plugin.msg("nchat-help-unmute"));
        sender.sendMessage(plugin.msg("nchat-help-nchat"));
        sender.sendMessage(plugin.msg("nchat-help-reload"));
        sender.sendMessage(plugin.msg("nchat-help-reload-ai"));
        sender.sendMessage(plugin.msg("nchat-help-op-add"));
        sender.sendMessage(plugin.msg("nchat-help-op-remove"));
        sender.sendMessage(plugin.msg("nchat-help-op-list"));
        sender.sendMessage(plugin.msg("nchat-help-github"));
        sender.sendMessage(plugin.msg("nchat-help-author"));
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }

        // /nchat reload ai → 仅重载 AI 配置
        if (args.length >= 2 && args[1].equalsIgnoreCase("ai")) {
            plugin.reloadConfig();
            plugin.getAIManager().reload();
            sender.sendMessage(plugin.msg("reload-ai-only"));
            plugin.getLogger().info("§6[NekoChat]§r " + sender.getName() + " 重载了 AI 配置");
            return;
        }

        // /nchat reload → 重载所有配置
        plugin.reloadConfig();
        plugin.getCBanManager().reload();
        plugin.getDatabaseManager().reload();
        plugin.getAIManager().reload();
        plugin.getMessageManager().reload();
        sender.sendMessage(plugin.msg("reload-all"));
        plugin.getLogger().info("§6[NekoChat]§r 管理员 " + sender.getName() + " 执行了全量重载");
    }

    private void handleOp(CommandSender sender, String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.msg("op-usage"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("op-usage"));
                    return;
                }
                String targetName = args[2];
                plugin.getOpManager().add(targetName);
                sender.sendMessage(plugin.msg("op-added", targetName));
                plugin.getLogger().info("§6[NekoChat]§r 管理员 " + sender.getName() + " 添加了管理员: " + targetName);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("op-usage"));
                    return;
                }
                String targetName = args[2];
                if (!plugin.getOpManager().isOp(targetName)) {
                    sender.sendMessage(plugin.msg("op-not-op", targetName));
                    return;
                }
                plugin.getOpManager().remove(targetName);
                sender.sendMessage(plugin.msg("op-removed", targetName));
                plugin.getLogger().info("§6[NekoChat]§r 管理员 " + sender.getName() + " 移除了管理员: " + targetName);
            }
            case "list" -> {
                Set<String> ops = plugin.getOpManager().getOps();
                if (ops.isEmpty()) {
                    sender.sendMessage(plugin.msg("op-help-empty"));
                } else {
                    sender.sendMessage(plugin.msg("op-help-header", String.valueOf(ops.size())));
                    for (String name : ops) {
                        sender.sendMessage(plugin.msg("op-help-item", name));
                    }
                }
            }
            default -> {
                sender.sendMessage(plugin.msg("op-unknown"));
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

        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            if (!plugin.isAdmin(sender)) return List.of();
            if ("ai".startsWith(args[1].toLowerCase())) {
                return List.of("ai");
            }
            return List.of();
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
