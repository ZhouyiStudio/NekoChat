package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TitleManager {

    private final NekoChat plugin;
    private final Map<UUID, String> titles = new HashMap<>();
    private File dataFile;
    private FileConfiguration data;

    public TitleManager(NekoChat plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 为玩家设置头衔
     */
    public void setTitle(Player player, String title) {
        titles.put(player.getUniqueId(), title);
        save();
    }

    /**
     * 移除玩家头衔
     */
    public void removeTitle(Player player) {
        titles.remove(player.getUniqueId());
        save();
    }

    /**
     * 获取玩家头衔（原始字符串，含颜色代码）
     */
    public String getTitleRaw(Player player) {
        return titles.get(player.getUniqueId());
    }

    /**
     * 获取玩家是否有头衔
     */
    public boolean hasTitle(Player player) {
        return titles.containsKey(player.getUniqueId());
    }

    /**
     * 获取头衔聊天前缀组件（格式: [头衔]）
     */
    public Component getChatPrefix(Player player) {
        String title = titles.get(player.getUniqueId());
        if (title == null) return null;

        String format = plugin.getConfig().getString("title-format", "&7[&r{title}&7]")
                .replace("{title}", title);

        return plugin.colorize(format);
    }

    /**
     * 获取 Tab 列表显示名称（格式: [头衔] 玩家名）
     */
    public Component getTabName(Player player) {
        String title = titles.get(player.getUniqueId());
        if (title == null) return player.name();

        String format = plugin.getConfig().getString("title-format", "&7[&r{title}&7]")
                .replace("{title}", title);

        return plugin.colorize(format).append(Component.space()).append(player.name());
    }

    /**
     * 从文件加载数据
     */
    private void load() {
        dataFile = new File(plugin.getDataFolder(), "titles.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("§c[NekoChat-ERROR]§r 无法创建 titles.yml: " + e.getMessage());
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String title = data.getString(key);
                if (title != null) {
                    titles.put(uuid, title);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§e[NekoChat-WARN]§r 无效的UUID: " + key);
            }
        }
    }

    /**
     * 保存数据到文件
     */
    public void save() {
        if (data == null) return;
        for (Map.Entry<UUID, String> entry : titles.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c[NekoChat-ERROR]§r 无法保存 titles.yml: " + e.getMessage());
        }
    }
}
