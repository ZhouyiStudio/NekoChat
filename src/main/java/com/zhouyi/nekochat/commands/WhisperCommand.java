package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理私聊命令 /msg /tell /w /whisper
 * 记录到数据库后交由服务器原生逻辑处理
 */
public class WhisperCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    public WhisperCommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.colorize("&c只有玩家可以使用私聊命令！"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&c用法: /" + label + " <玩家> <消息>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.colorize("&c玩家 " + args[0] + " 不在线！"));
            return true;
        }

        // 拼接消息内容
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        String message = sb.toString();

        // 记录到数据库
        if (plugin.getDatabaseManager().isEnabled()) {
            plugin.getDatabaseManager().logPrivateChat(
                    player.getName(), player.getUniqueId().toString(),
                    target.getName(), message
            );
        }

        // 发送私聊（模拟原版格式）
        Component outgoing = plugin.colorize("&7[你 -> &e" + target.getName() + "&7] &f" + message);
        Component incoming = plugin.colorize("&7[&e" + player.getName() + " &7-> 你] &f" + message);

        player.sendMessage(outgoing);
        target.sendMessage(incoming);

        // 播放音效提示（可选）
        // target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        return true;
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
        return List.of();
    }
}
