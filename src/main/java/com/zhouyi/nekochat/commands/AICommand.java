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
import java.util.Set;
import java.util.stream.Collectors;

public class AICommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

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

        // 拼装消息
        String message = String.join(" ", args);

        // 控制台直接发送
        if (!(sender instanceof Player player)) {
            plugin.getAIManager().askAI("Console", message).thenAccept(response -> {
                sender.sendMessage(plugin.colorize(response));
            });
            return true;
        }

        // 检查冷却
        if (plugin.getAIManager().isOnCooldown(player.getUniqueId())) {
            int remaining = plugin.getAIManager().getCooldownRemaining(player.getUniqueId());
            player.sendMessage(plugin.colorize("&c请等待 " + remaining + " 秒后再使用 AI 助手！"));
            return true;
        }

        // 检查第一个词是否是自定义提示词
        String firstWord = args[0].toLowerCase();
        String promptName = plugin.getAIManager().getPrompt(firstWord);
        String userMessage;
        String finalPrompt = null;

        if (promptName != null) {
            // 使用了自定义提示词
            finalPrompt = promptName;
            if (args.length > 1) {
                userMessage = message.substring(firstWord.length()).trim();
            } else {
                userMessage = "";
            }
        } else {
            userMessage = message;
        }

        if (userMessage.isEmpty()) {
            sender.sendMessage(plugin.colorize("&c请输入要提问的内容！"));
            return true;
        }

        // 发送 AI 请求
        sendAIMessage(player, userMessage, finalPrompt, firstWord);
        return true;
    }

    /**
     * 发送 AI 消息并处理回复
     */
    private void sendAIMessage(Player player, String message, String customPrompt, String promptHint) {
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
        plugin.getAIManager().askAI(player.getName(), message, customPrompt).thenAccept(response -> {
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
        sender.sendMessage(plugin.colorize("&e/ai <提示词> <消息> &7- 使用指定提示词提问"));
        // 列出可用的自定义提示词
        Set<String> prompts = plugin.getAIManager().getPromptNames();
        if (!prompts.isEmpty()) {
            sender.sendMessage(plugin.colorize("&7可用提示词: &e" + String.join("&7, &e", prompts)));
        }
        sender.sendMessage(plugin.colorize("&e@ai <消息> &7- 在聊天框直接 @ai 提问"));
        sender.sendMessage(plugin.colorize("&e/nchat reload &7- 重载所有配置（含 AI）"));
        sender.sendMessage(plugin.colorize("&8支持任何 OpenAI 兼容 API"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // 提示自定义提示词
            Set<String> prompts = plugin.getAIManager().getPromptNames();
            return prompts.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
