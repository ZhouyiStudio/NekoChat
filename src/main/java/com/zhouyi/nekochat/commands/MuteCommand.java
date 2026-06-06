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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d+)([sSmMhHdD])?"
    );

    public MuteCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.colorize("&c用法: /mute <玩家> [时间]"));
            sender.sendMessage(plugin.colorize("&7时间格式: 数字+单位(s/m/h/d)，例如: 30s, 5m, 2h, 1d"));
            sender.sendMessage(plugin.colorize("&7不写时间则为永久禁言"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.colorize("&c玩家 " + args[0] + " 不在线！"));
            return true;
        }

        // 检查目标是否有 bypass 权限
        if (target.hasPermission("nekochat.bypass.mute")) {
            sender.sendMessage(plugin.colorize("&c无法禁言该玩家！"));
            return true;
        }

        long duration = -1; // 默认永久
        if (args.length >= 2) {
            Matcher matcher = TIME_PATTERN.matcher(args[1]);
            if (matcher.matches()) {
                long amount = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2);
                if (unit == null) unit = "s"; // 默认秒

                duration = switch (unit.toLowerCase()) {
                    case "s" -> amount;
                    case "m" -> amount * 60;
                    case "h" -> amount * 3600;
                    case "d" -> amount * 86400;
                    default -> amount;
                };
            } else {
                sender.sendMessage(plugin.colorize("&c时间格式无效！示例: 30s, 5m, 2h, 1d"));
                return true;
            }
        }

        plugin.getMuteManager().mute(target, duration);

        if (duration == -1) {
            Bukkit.broadcast(plugin.colorize("&c" + target.getName() + " &e已被永久禁言！"));
        } else {
            String timeStr = formatDuration(duration);
            Bukkit.broadcast(plugin.colorize("&c" + target.getName() + " &e已被禁言 " + timeStr + "！"));
        }

        return true;
    }

    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        if (secs > 0 || sb.isEmpty()) sb.append(secs).append("秒");
        return sb.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("30s", "1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "7d");
        }
        return List.of();
    }
}
