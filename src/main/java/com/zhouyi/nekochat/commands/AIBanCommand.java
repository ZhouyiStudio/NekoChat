package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AIBanCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public AIBanCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // 只有管理员（通过 /nchat op add 添加的）可以使用
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.msg("aiban-help-header"));
            sender.sendMessage(plugin.msg("aiban-help-ban"));
            sender.sendMessage(plugin.msg("aiban-help-remove"));
            sender.sendMessage(plugin.msg("aiban-help-list"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("aiban-usage-remove"));
                    return true;
                }
                String target = args[1];
                if (!plugin.getAIManager().isAiBanned(target)) {
                    sender.sendMessage(plugin.msg("aiban-not-banned", target));
                    return true;
                }
                plugin.getAIManager().unbanPlayer(target);
                sender.sendMessage(plugin.msg("aiban-unbanned", target));
                plugin.getLogger().info("§6[NekoChat]§r 管理员 " + sender.getName() + " 解除了 " + target + " 的 AI 封禁");
            }
            case "list" -> {
                var banned = plugin.getAIManager().getAiBannedPlayers();
                if (banned.isEmpty()) {
                    sender.sendMessage(plugin.msg("aiban-list-empty"));
                } else {
                    sender.sendMessage(plugin.msg("aiban-list-header", String.valueOf(banned.size())));
                    for (String name : banned) {
                        sender.sendMessage(plugin.msg("aiban-list-item", name));
                    }
                }
            }
            default -> {
                // /aiban <玩家>
                String target = args[0];
                if (plugin.getAIManager().isAiBanned(target)) {
                    sender.sendMessage(plugin.msg("aiban-already-banned", target));
                    return true;
                }
                plugin.getAIManager().banPlayer(target);
                sender.sendMessage(plugin.msg("aiban-banned", target));
                plugin.getLogger().info("§6[NekoChat]§r 管理员 " + sender.getName() + " 封禁了 " + target + " 的 AI 使用权限");
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!plugin.isAdmin(sender)) return List.of();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("remove", "list"));
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("del"))) {
            return plugin.getAIManager().getAiBannedPlayers().stream()
                    .filter(n -> n.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
