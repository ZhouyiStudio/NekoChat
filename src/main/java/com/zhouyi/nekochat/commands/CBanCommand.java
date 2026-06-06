package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CBanCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public CBanCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.colorize("&c你没有权限执行此命令！"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.colorize("&6/cban add <词汇> &e- 添加屏蔽词"));
            sender.sendMessage(plugin.colorize("&6/cban remove <词汇> &e- 移除屏蔽词"));
            sender.sendMessage(plugin.colorize("&6/cban list &e- 查看所有屏蔽词"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.colorize("&c请指定要屏蔽的词汇！用法: /cban add <词汇>"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String word = sb.toString().toLowerCase();
                plugin.getCBanManager().addWord(word);
                sender.sendMessage(plugin.colorize("&a已添加屏蔽词: &e" + word));
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.colorize("&c请指定要移除的词汇！用法: /cban remove <词汇>"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String word = sb.toString().toLowerCase();
                plugin.getCBanManager().removeWord(word);
                sender.sendMessage(plugin.colorize("&a已移除屏蔽词: &e" + word));
            }
            case "list" -> {
                List<String> words = plugin.getCBanManager().getBannedWords();
                if (words.isEmpty()) {
                    sender.sendMessage(plugin.colorize("&e当前没有屏蔽词。"));
                } else {
                    sender.sendMessage(plugin.colorize("&6=== 屏蔽词列表 (" + words.size() + "个) ==="));
                    for (String w : words) {
                        sender.sendMessage(plugin.colorize("&7- &e" + w));
                    }
                }
            }
            default -> {
                sender.sendMessage(plugin.colorize("&c未知子命令！可用: add, remove, list"));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("del"))) {
            return plugin.getCBanManager().getBannedWords().stream()
                    .filter(w -> w.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
