package com.zhouyi.nekochat.listeners;

import com.zhouyi.nekochat.NekoChat;
import com.zhouyi.nekochat.managers.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * 监听 /say 命令，将服务器消息中的 URL 转为可点击并解析 & 颜色代码
 */
public class ServerMessageListener implements Listener {

    private final NekoChat plugin;

    public ServerMessageListener(NekoChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand().trim();
        if (!cmd.toLowerCase().startsWith("say ")) return;

        String message = cmd.substring(4).trim();
        if (message.isEmpty()) return;

        // 记录到数据库
        DatabaseManager db = plugin.getDatabaseManager();
        if (db.isEnabled()) {
            db.logServerMessage(message);
            if (plugin.isDebug()) {
                plugin.getLogger().info("§7[NekoChat-DEBUG]§r 已写入服务器消息: " + message);
            }
        }

        // 处理消息：解析 & 颜色代码 + URL 转可点击
        Component processed = plugin.processMessage(message);

        // 取消原命令，手动广播
        event.setCancelled(true);
        Bukkit.broadcast(processed);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().trim();
        String lowerCmd = cmd.toLowerCase();

        // 处理 /say 命令
        if (lowerCmd.startsWith("/say ")) {
            Player player = event.getPlayer();
            String message = cmd.substring(5).trim();
            if (message.isEmpty()) return;

            // 记录到数据库
            DatabaseManager db = plugin.getDatabaseManager();
            if (db.isEnabled()) {
                db.logPlayerChat(player.getName(), player.getUniqueId().toString(), "/say " + message);
                if (plugin.isDebug()) {
                    plugin.getLogger().info("§7[NekoChat-DEBUG]§r 已写入玩家 /say: " + player.getName() + " -> " + message);
                }
            }

            // 处理消息：解析 & 颜色代码 + URL 转可点击
            Component processed = plugin.processMessage(message);

            // 取消原命令，手动广播
            event.setCancelled(true);
            Bukkit.broadcast(processed);
            return;
        }

        // 处理私聊命令：/msg, /tell, /w, /whisper, /minecraft:msg, /minecraft:tell, /r
        // 格式: /msg <玩家> <消息>
        String[] whisperPrefixes = {"/msg ", "/tell ", "/w ", "/whisper ", "/minecraft:msg ", "/minecraft:tell "};
        for (String prefix : whisperPrefixes) {
            if (lowerCmd.startsWith(prefix)) {
                String rest = cmd.substring(prefix.length()).trim();
                if (rest.isEmpty()) return;

                // 提取接收者玩家名和消息内容
                int spaceIdx = rest.indexOf(' ');
                if (spaceIdx == -1) return; // 没有消息内容，只是 /msg <player>

                String targetPlayer = rest.substring(0, spaceIdx);
                String message = rest.substring(spaceIdx + 1).trim();
                if (message.isEmpty()) return;

                Player player = event.getPlayer();

                // 记录到数据库，类型为 whisper
                DatabaseManager db = plugin.getDatabaseManager();
                if (db.isEnabled()) {
                    db.logPrivateChat(player.getName(), player.getUniqueId().toString(), targetPlayer, message);
                    if (plugin.isDebug()) {
                        plugin.getLogger().info("§7[NekoChat-DEBUG]§r 已写入私聊记录: " + player.getName() + " -> " + targetPlayer + " : " + message);
                    }
                }
                break;
            }
        }
    }
}
