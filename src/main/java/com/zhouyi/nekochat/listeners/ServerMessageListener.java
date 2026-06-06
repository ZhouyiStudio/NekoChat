package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
import io.papermc.paper.event.message.ServerMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 监听服务器消息（/say、控制台发送等），自动渲染 URL 和颜色代码
 */
public class ServerMessageListener implements Listener {

    private final NekoChat plugin;

    public ServerMessageListener(NekoChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerMessage(ServerMessageEvent event) {
        // 不处理系统内部消息（如插件日志、后台输出等）
        // ServerMessageEvent 默认只处理发送给玩家的服务器消息（/say 等）
        Component original = event.message();
        String plain = PlainTextComponentSerializer.plainText().serialize(original);

        // 检查是否包含 URL 或 & 颜色代码
        boolean hasUrl = NekoChat.URL_PATTERN.matcher(plain).find();
        boolean hasColor = plain.contains("&");

        if (!hasUrl && !hasColor) {
            return; // 无需处理
        }

        // 处理消息：解析 & 颜色代码 + URL 转可点击
        Component processed = plugin.processMessage(plain);
        event.message(processed);
    }
}
