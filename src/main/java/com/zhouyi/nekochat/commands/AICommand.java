package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.stream.Stream;

public class AICommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    private static final String PREFIX = "&b[AI助手] ";
    private static final String SEPARATOR = "&7&m----------------------------------------------------";

    public AICommand(NekoChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // 检查 AI 是否启用
        if (!plugin.getAIManager().isEnabled()) {
            sender.sendMessage(plugin.colorize("&cAI 功能未启用！请在 config.yml 中配置并启用。"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        // 处理子命令
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                // 只有管理员可以重载
                if (!plugin.isAdmin(sender)) {
                    sender.sendMessage(plugin.colorize("&c你没有权限执行此命令！"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getAIManager().reload();
                sender.sendMessage(plugin.colorize("&aAI 配置已重新加载！"));
                plugin.getLogger().info("§6[NekoChat]§r " + sender.getName() + " 重载了 AI 配置");
                return true;
            }
            default -> {
                // 只有玩家可以使用 /ai <消息>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.colorize("&c控制台不能直接发送 AI 消息，请使用 /ai reload"));
                    return true;
                }

                // 拼装消息
                String message = String.join(" ", args);

                // 检查冷却
                if (plugin.getAIManager().isOnCooldown(player.getUniqueId())) {
                    int remaining = plugin.getAIManager().getCooldownRemaining(player.getUniqueId());
                    player.sendMessage(plugin.colorize("&c请等待 " + remaining + " 秒后再使用 AI 助手！"));
                    return true;
                }

                // 发送 AI 请求
                sendAIMessage(player, message);
                return true;
            }
        }
    }

    /**
     * 发送 AI 消息并处理回复
     */
    private void sendAIMessage(Player player, String message) {
        // 记录冷却
        plugin.getAIManager().recordUsage(player.getUniqueId());

        // 通知玩家请求已发送
        player.sendMessage(plugin.colorize("&7AI 思考中，请稍候..."));

        // 如果公聊模式下，通知所有玩家
        if (plugin.getAIManager().isPublicReply()) {
            Component publicNotice = plugin.colorize("&e" + player.getName() + " &7向 AI 提出了问题...");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.sendMessage(publicNotice);
                }
            }
        }

        // 异步请求 AI
        plugin.getAIManager().askAI(player.getName(), message).thenAccept(response -> {
            // 回到主线程执行
            Bukkit.getScheduler().runTask(plugin, () -> {
                String displayName = plugin.getAIManager().getDisplayName();

                if (plugin.getAIManager().isPublicReply()) {
                    // 公聊：广播给所有人
                    Component broadcastMsg = plugin.colorize(displayName + "&r " + response);
                    Bukkit.broadcast(broadcastMsg);
                } else {
                    // 私聊：只发给提问者
                    player.sendMessage(plugin.colorize(displayName + "&r " + response));
                }
            });
        });
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6=== NekoChat AI 助手 ==="));
        sender.sendMessage(plugin.colorize("&e/ai <消息> &7- 向 AI 提问"));
        sender.sendMessage(plugin.colorize("&e/ai reload &7- 重新加载 AI 配置（管理员）"));
        sender.sendMessage(plugin.colorize("&8支持任何 OpenAI 兼容 API"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
