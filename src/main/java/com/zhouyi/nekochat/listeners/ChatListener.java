package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
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

    // 默认玩家名称颜色（浅灰色）
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

        // 获取纯文本消息内容
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).toLowerCase();

        // 2. 检查屏蔽词
        if (!player.hasPermission("nekochat.bypass.cban")) {
            if (plugin.getCBanManager().containsBannedWord(message)) {
                String warnMsg = plugin.getConfig().getString("cban-message", "&c消息包含违规词汇，已拦截！");
                player.sendMessage(plugin.colorize(warnMsg));
                event.setCancelled(true);
                return;
            }
        }

        // 3. 使用自定义渲染器，将头衔显示在消息中
        boolean hasTitle = plugin.getTitleManager().hasTitle(player);
        Component titleComponent = hasTitle
                ? plugin.getTitleManager().getChatPrefix(player)
                : null;

        event.renderer((source, sourceDisplayName, messageToSend, viewer) -> {
            Component displayName = sourceDisplayName;

            if (titleComponent != null) {
                // 格式: [头衔] <玩家名> 消息
                return titleComponent
                        .append(SPACE)
                        .append(ANGLE_BRACKET_LEFT)
                        .append(displayName)
                        .append(ANGLE_BRACKET_RIGHT)
                        .append(messageToSend);
            }

            // 无头衔: <玩家名> 消息
            return ANGLE_BRACKET_LEFT
                    .append(displayName)
                    .append(ANGLE_BRACKET_RIGHT)
                    .append(messageToSend);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 玩家上线时更新 Tab 列表显示头衔
        if (plugin.getTitleManager().hasTitle(player)) {
            player.playerListName(plugin.getTitleManager().getTabName(player));
        }
    }
}
