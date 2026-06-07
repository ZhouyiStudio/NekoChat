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
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.msg("cban-help-header"));
            sender.sendMessage(plugin.msg("cban-help-add"));
            sender.sendMessage(plugin.msg("cban-help-remove"));
            sender.sendMessage(plugin.msg("cban-help-list"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("cban-usage-add"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String word = sb.toString().toLowerCase();
                plugin.getCBanManager().addWord(word);
                sender.sendMessage(plugin.msg("cban-added", word));
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("cban-usage-remove"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String word = sb.toString().toLowerCase();
                plugin.getCBanManager().removeWord(word);
                sender.sendMessage(plugin.msg("cban-removed", word));
            }
            case "list" -> {
                List<String> words = plugin.getCBanManager().getBannedWords();
                if (words.isEmpty()) {
                    sender.sendMessage(plugin.msg("cban-list-empty"));
                } else {
                    sender.sendMessage(plugin.msg("cban-list-header", String.valueOf(words.size())));
                    for (String w : words) {
                        sender.sendMessage(plugin.msg("cban-list-item", w));
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
