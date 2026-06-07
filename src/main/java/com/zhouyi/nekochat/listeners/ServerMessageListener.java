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
 * 私聊命令 (/msg /tell /w /whisper) 已由 WhisperCommand 接管
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

        DatabaseManager db = plugin.getDatabaseManager();
        if (db.isEnabled()) {
            db.logServerMessage(message);
            if (plugin.isDebug()) {
                plugin.getLogger().info("§7[NekoChat-DEBUG]§r 已写入服务器消息: " + message);
            }
        }

        String prefix = plugin.getConfig().getString("say-prefix", "&7[服务器]");
        Component processed = plugin.colorize(prefix + " &r").append(plugin.processMessage(message));
        event.setCancelled(true);
        Bukkit.broadcast(processed);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().trim();
        String lowerCmd = cmd.toLowerCase();

        if (lowerCmd.startsWith("/say ")) {
            Player player = event.getPlayer();
            String message = cmd.substring(5).trim();
            if (message.isEmpty()) return;

            DatabaseManager db = plugin.getDatabaseManager();
            if (db.isEnabled()) {
                db.logPlayerChat(player.getName(), player.getUniqueId().toString(), "/say " + message);
                if (plugin.isDebug()) {
                    plugin.getLogger().info("§7[NekoChat-DEBUG]§r 已写入玩家 /say: " + player.getName() + " -> " + message);
                }
            }

            Component processed = plugin.processMessage(message);
            event.setCancelled(true);
            String prefix = plugin.getConfig().getString("say-prefix", "&7[服务器]");
            Bukkit.broadcast(plugin.colorize(prefix + " &r").append(processed));
            return;
        }
    }
}
