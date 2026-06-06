package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PTitleCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public PTitleCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.colorize("&c只有玩家才能使用此命令！"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.colorize("&6/ptitle add <头衔> &e- 设置头衔（支持 & 颜色代码）"));
            player.sendMessage(plugin.colorize("&6/ptitle remove &e- 移除头衔"));
            player.sendMessage(plugin.colorize("&7示例: /ptitle add &b&l大神"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.colorize("&c请指定头衔！用法: /ptitle add <头衔>"));
                    return true;
                }

                // 拼接头衔（支持空格和颜色代码）
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                String title = sb.toString();

                // 限制头衔长度（防止过长）
                if (title.replace("&", "").replaceAll("[0-9a-fklmnor]", "").length() > 32) {
                    player.sendMessage(plugin.colorize("&c头衔过长！最大支持32个字符（不含颜色代码）。"));
                    return true;
                }

                plugin.getTitleManager().setTitle(player, title);
                // 更新 Tab 列表显示
                player.playerListName(plugin.getTitleManager().getTabName(player));
                player.sendMessage(plugin.colorize("&a头衔已设置为: " + title));
            }
            case "remove", "del", "delete", "clear" -> {
                plugin.getTitleManager().removeTitle(player);
                // 恢复 Tab 列表默认显示
                player.playerListName(player.name());
                player.sendMessage(plugin.colorize("&a头衔已移除！"));
            }
            default -> {
                player.sendMessage(plugin.colorize("&c未知子命令！可用: add, remove"));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            return completions;
        }
        // add 后面不补全
        return List.of();
    }
}
