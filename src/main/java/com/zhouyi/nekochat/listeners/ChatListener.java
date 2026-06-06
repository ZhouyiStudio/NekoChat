package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final NekoChat plugin;

    private static final Component ANGLE_BRACKET_LEFT = Component.text("<");
    private static final Component ANGLE_BRACKET_RIGHT = Component.text("> ");
    private static final Component SPACE = Component.space();

    // URL 正则
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w./?=&#%+~@,:;!-]+|www\\.[\\w./?=&#%+~@,:;!-]+",
            Pattern.CASE_INSENSITIVE
    );

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

        // 4. 构建带可点击 URL 的消息组件
        Component clickableMessage = makeClickableMessage(plainText);

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

    /**
     * 将纯文本中的 URL 替换为可点击的组件
     */
    private Component makeClickableMessage(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            // 没有 URL，直接返回纯文本
            return Component.text(text);
        }

        Component result = Component.empty();
        int lastEnd = 0;

        do {
            // URL 前面的普通文本
            if (matcher.start() > lastEnd) {
                result = result.append(Component.text(text.substring(lastEnd, matcher.start())));
            }

            String url = matcher.group();
            // 确保 URL 有协议前缀
            String clickUrl = url.startsWith("http") ? url : "https://" + url;

            Component urlComponent = Component.text(url)
                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(clickUrl))
                    .hoverEvent(plugin.colorize("&b点击打开: " + url));

            result = result.append(urlComponent);
            lastEnd = matcher.end();

        } while (matcher.find());

        // 剩余文本
        if (lastEnd < text.length()) {
            result = result.append(Component.text(text.substring(lastEnd)));
        }

        return result;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getTitleManager().hasTitle(player)) {
            player.playerListName(plugin.getTitleManager().getTabName(player));
        }
    }
}
