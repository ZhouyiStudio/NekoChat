package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
import com.zhouyi.nekochat.managers.DatabaseManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

        // 4. 异步记录聊天到数据库
        DatabaseManager db = plugin.getDatabaseManager();
        if (db.isEnabled()) {
            db.logPlayerChat(player.getName(), player.getUniqueId().toString(), plainText);
            plugin.getLogger().fine("已写入聊天记录: " + player.getName() + " -> " + plainText);
        } else {
            plugin.getLogger().warning("数据库未启用，跳过写入: " + player.getName() + " -> " + plainText);
        }

        // 5. 构建带颜色代码 + 可点击 URL 的消息组件
        Component clickableMessage = plugin.processMessage(plainText);

        // 5. 使用自定义渲染器：头衔 + 玩家名 + 可点击消息
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTitleManager().hasTitle(player)) {
            player.playerListName(plugin.getTitleManager().getTabName(player));
        }
    }
}
