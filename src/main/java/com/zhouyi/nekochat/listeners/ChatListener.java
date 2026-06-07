package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
import com.zhouyi.nekochat.managers.AIManager;
import com.zhouyi.nekochat.managers.DatabaseManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChatListener implements Listener {

    private final NekoChat plugin;

    private static final Component ANGLE_BRACKET_LEFT = Component.text("<");
    private static final Component ANGLE_BRACKET_RIGHT = Component.text("> ");
    private static final Component SPACE = Component.space();

    public ChatListener(NekoChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // 1. 检查禁言
        if (!player.hasPermission("nekochat.bypass.mute")) {
            if (plugin.getMuteManager().checkAndNotify(player)) {
                event.setCancelled(true);
                return;
            }
        }

        // 获取纯文本消息内容（小写用于检测）
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
        String lowerText = plainText.toLowerCase();

        // 2. 检查屏蔽词
        if (!player.hasPermission("nekochat.bypass.cban")) {
            if (plugin.getCBanManager().containsBannedWord(lowerText)) {
                String warnMsg = plugin.getConfig().getString("cban-message", "&c消息包含违规词汇，已拦截！");
                player.sendMessage(plugin.colorize(warnMsg));
                event.setCancelled(true);
                return;
            }
        }

        // 3. 检查服务器宣传
        if (!player.hasPermission("nekochat.bypass.cban")) {
            String ad = plugin.getCBanManager().containsServerAd(lowerText);
            if (ad != null) {
                String warnMsg = plugin.getConfig().getString("server-ad-message", "&c请勿宣传其他服务器！");
                player.sendMessage(plugin.colorize(warnMsg));
                event.setCancelled(true);
                return;
            }
        }

        // 4. 检测 @ai / @AI 提及 → 触发 AI 助手
        if (plugin.getAIManager().isEnabled() && AIManager.isAiMention(plainText)) {
            handleAiMention(player, plainText, event);
            return; // 事件已取消，直接返回
        }

        // 5. 异步记录聊天到数据库
        DatabaseManager db = plugin.getDatabaseManager();
        if (db.isEnabled()) {
            db.logPlayerChat(player.getName(), player.getUniqueId().toString(), plainText);
            if (plugin.isDebug()) {
                plugin.getLogger().info("§7[NekoChat-DEBUG]§r 聊天记录已保存: " + player.getName() + " -> " + plainText);
            }
        } else if (plugin.isDebug()) {
            plugin.getLogger().info("§7[NekoChat-DEBUG]§r 数据库未启用，跳过聊天记录保存: " + player.getName() + " -> " + plainText);
        }

        // 6. 构建带颜色代码 + 可点击 URL 的消息组件
        Component clickableMessage = plugin.processMessage(plainText);

        // 6. 使用自定义渲染器：头衔 + 玩家名 + 可点击消息
        boolean hasTitle = plugin.getTitleManager().hasTitle(player);
        Component titleComponent = hasTitle
                ? plugin.getTitleManager().getChatPrefix(player)
                : null;

        event.renderer((source, sourceDisplayName, messageToSend, viewer) -> {
            // 使用我们构建的可点击消息，忽略原始的 messageToSend
            Component displayName = sourceDisplayName;

            Component base;
            if (titleComponent != null) {
                base = titleComponent.append(SPACE)
                        .append(ANGLE_BRACKET_LEFT)
                        .append(displayName)
                        .append(ANGLE_BRACKET_RIGHT);
            } else {
                base = ANGLE_BRACKET_LEFT
                        .append(displayName)
                        .append(ANGLE_BRACKET_RIGHT);
            }

            return base.append(clickableMessage);
        });
    }

    /**
     * 处理 @ai / @AI 触发 AI 助手的消息
     */
    private void handleAiMention(Player player, String plainText, AsyncChatEvent event) {
        // 取消原始聊天事件
        event.setCancelled(true);

        // 提取 @ai 后的内容
        String content = plainText.trim().substring(3).trim();
        if (content.isEmpty()) {
            player.sendMessage(plugin.colorize("&c用法: @ai <消息> 或 @ai <提示词> <消息>"));
            return;
        }

        // 检查冷却
        if (plugin.getAIManager().isOnCooldown(player.getUniqueId())) {
            int remaining = plugin.getAIManager().getCooldownRemaining(player.getUniqueId());
            player.sendMessage(plugin.colorize("&c请等待 " + remaining + " 秒后再使用 AI 助手！"));
            return;
        }

        // 检查 AI 封禁
        if (plugin.getAIManager().isAiBanned(player.getName())) {
            player.sendMessage(plugin.colorize("&c你已被禁止使用 AI 助手！"));
            return;
        }

        // 检查每日使用限制
        if (plugin.getAIManager().isDailyLimitReached(player.getUniqueId())) {
            int limit = plugin.getAIManager().getDailyLimit();
            player.sendMessage(plugin.colorize("&c你今天使用 AI 已达上限（" + limit + "次），明天再试吧！"));
            return;
        }

        // 检查是否有自定义提示词（第一个词）
        String[] parts = content.split(" ", 2);
        String firstWord = parts[0].toLowerCase();
        String promptContent = plugin.getAIManager().getPrompt(firstWord);
        String userMessage;
        String finalPrompt = null;
        String promptHint = null;

        if (promptContent != null) {
            // 使用了自定义提示词
            finalPrompt = promptContent;
            promptHint = firstWord;
            userMessage = (parts.length > 1) ? parts[1].trim() : "";
        } else {
            userMessage = content;
        }

        if (userMessage.isEmpty()) {
            player.sendMessage(plugin.colorize("&c请输入要提问的内容！"));
            return;
        }

        // 记录冷却和每日使用
        plugin.getAIManager().recordUsage(player.getUniqueId());
        plugin.getAIManager().incrementDailyUsage(player.getUniqueId());

        // 广播玩家的消息（@AI 部分显示为绿色）
        String displayName = plugin.getAIManager().getDisplayName();
        String broadcastText = (promptHint != null)
                ? "&e" + player.getName() + " &a@AI " + promptHint + " &f" + userMessage
                : "&e" + player.getName() + " &a@AI &f" + userMessage;
        Bukkit.broadcast(plugin.colorize(broadcastText));

        // 通知提问者 AI 思考中
        player.sendMessage(plugin.colorize("&7AI 思考中，请稍候..."));

        // 异步请求 AI
        plugin.getAIManager().askAI(player.getName(), userMessage, finalPrompt).thenAccept(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Component replyMsg = plugin.colorize(displayName + "&r " + response);
                Bukkit.broadcast(replyMsg);
            });
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTitleManager().hasTitle(player)) {
            player.playerListName(plugin.getTitleManager().getTabName(player));
        }
    }
}
