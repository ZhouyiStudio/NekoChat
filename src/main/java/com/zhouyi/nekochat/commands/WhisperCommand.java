package com.zhouyi.nekochat.commands;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

public class WhisperCommand implements CommandExecutor, TabCompleter {

    private final NekoChat plugin;

    private static final TextColor NAME_COLOR = TextColor.color(0x55FF55);   // 亮绿色
    private static final TextColor ARROW_COLOR = NamedTextColor.GRAY;        // 灰色箭头
    private static final TextColor ME_COLOR = TextColor.color(0x55FFFF);     // 青色"我"
    private static final TextColor MSG_COLOR = NamedTextColor.WHITE;         // 白色消息

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
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // 记录到数据库
        if (plugin.getDatabaseManager().isEnabled()) {
            plugin.getDatabaseManager().logPrivateChat(
                    player.getName(), player.getUniqueId().toString(),
                    target.getName(), msg
            );
        }

        String senderName = player.getName();
        String targetName = target.getName();

        // ====== 发送方看到: 我 → 玩家名 ======
        player.sendMessage(buildWhisperLine(senderName, targetName, msg, false));

        // ====== 接收方看到: 玩家名 → 我 ======
        target.sendMessage(buildWhisperLine(senderName, targetName, msg, true));

        return true;
    }

    /**
     * 构建私聊消息行
     *
     * @param senderName 发送方
     * @param targetName 接收方
     * @param msg        消息内容
     * @param isIncoming true=接收方视角, false=发送方视角
     */
    private Component buildWhisperLine(String senderName, String targetName, String msg, boolean isIncoming) {
        String leftName  = isIncoming ? senderName : "我";
        String rightName = isIncoming ? "我" : targetName;
        String arrow     = " → ";
        String clickTarget = isIncoming ? senderName : targetName;

        // 左侧名字 — 点击回复
        Component namePart = Component.text(leftName)
                .color(isIncoming ? NAME_COLOR : ME_COLOR)
                .hoverEvent(HoverEvent.showText(
                        Component.text("§a点击回复 " + clickTarget)
                ))
                .clickEvent(ClickEvent.suggestCommand("/msg " + clickTarget + " "));

        // 箭头
        Component arrowPart = Component.text(arrow).color(ARROW_COLOR);

        // 右侧名字（我/对方） — 点击发送 tpa
        Component rightPart = Component.text(rightName)
                .color(isIncoming ? ME_COLOR : NAME_COLOR)
                .hoverEvent(HoverEvent.showText(
                        Component.text("§a向 " + clickTarget + " 发送传送请求")
                ))
                .clickEvent(ClickEvent.suggestCommand("/tpa " + clickTarget));

        // 消息内容 — 点击复制
        Component msgPart = Component.text(msg)
                .color(MSG_COLOR)
                .hoverEvent(HoverEvent.showText(
                        Component.text("§7点击复制消息")
                ))
                .clickEvent(ClickEvent.copyToClipboard(msg));

        return Component.empty()
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(namePart)
                .append(arrowPart)
                .append(rightPart)
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(msgPart);
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
