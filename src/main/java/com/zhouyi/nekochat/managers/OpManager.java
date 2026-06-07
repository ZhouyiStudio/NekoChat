package com.zhouyi.nekochat.managers;

import com.zhouyi.nekochat.NekoChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 自定义 OP 管理器
 * 通过 /nchat op add/remove 管理可以使用插件管理命令的玩家列表
 */
public class OpManager {

    private final NekoChat plugin;
    private final Set<String> ops = new LinkedHashSet<>();
    private File dataFile;
    private FileConfiguration data;

    public OpManager(NekoChat plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 添加 OP
     */
    public void add(String playerName) {
        ops.add(playerName.toLowerCase());
        save();
    }

    /**
     * 移除 OP
     */
    public void remove(String playerName) {
        ops.remove(playerName.toLowerCase());
        save();
    }

    /**
     * 检查是否为 OP
     */
    public boolean isOp(String playerName) {
        return ops.contains(playerName.toLowerCase());
    }

    /**
     * 获取所有 OP 列表（只读）
     */
    public Set<String> getOps() {
        return Collections.unmodifiableSet(ops);
    }

    /**
     * 从文件加载数据
     */
    private void load() {
        dataFile = new File(plugin.getDataFolder(), "ops.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("§c[NekoChat-ERROR]§r 无法创建 ops.yml: " + e.getMessage());
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            if (data.getBoolean(key, false)) {
                ops.add(key.toLowerCase());
            }
        }
    }

    /**
     * 保存数据到文件
     */
    public void save() {
        if (data == null) return;
        // 清除旧数据
        for (String key : data.getKeys(false)) {
            data.set(key, null);
        }
        // 写入当前数据
        for (String name : ops) {
            data.set(name, true);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c[NekoChat-ERROR]§r 无法保存 ops.yml: " + e.getMessage());
        }
    }
}
