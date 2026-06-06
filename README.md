# NekoChat

一个适用于 Purpur 1.21.1 (Paper API) 的聊天管理插件，支持头衔、屏蔽词和禁言功能。

## 功能

### 🏷️ 头衔系统 (`/ptitle`)
- `/ptitle add <头衔>` — 设置头衔，支持 `&` 颜色代码（如 `&b&l⚡大神`）
- `/ptitle remove` — 移除头衔
- 头衔会显示在 **聊天消息** 和 **Tab 列表** 中

### 🚫 屏蔽词系统 (`/cban`)
- `/cban add <词汇>` — 添加屏蔽词
- `/cban remove <词汇>` — 移除屏蔽词
- `/cban list` — 查看所有屏蔽词
- 默认内置 14 个中文 + 14 个英文常见屏蔽词
- 含屏蔽词的消息会被自动拦截

### 🔇 禁言系统 (`/mute` / `/unmute`)
- `/mute <玩家> [时间]` — 禁言玩家
- `/unmute <玩家>` — 解除禁言
- 时间格式: `30s`, `5m`, `2h`, `1d`（不写时间 = 永久禁言）
- 到期自动解除

## 权限

| 权限 | 默认 | 说明 |
|------|------|------|
| `nekochat.ptitle` | 所有玩家 | 使用头衔功能 |
| `nekochat.cban` | OP | 管理屏蔽词 |
| `nekochat.mute` | OP | 禁言/解禁玩家 |
| `nekochat.bypass.cban` | OP | 绕过屏蔽词检测 |
| `nekochat.bypass.mute` | OP | 绕过禁言 |

## 构建

### 环境要求
- JDK 21
- Gradle 8.7+

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/ZhouyiStudio/NekoChat.git
cd NekoChat

# 构建插件
./gradlew build

# 产物在 build/libs/NekoChat-1.0.0.jar
```

## 配置

`plugins/NekoChat/config.yml`:

```yaml
# 禁言提示
mute-message: "&c你已被禁言{reason}&c！"

# 屏蔽词提示
cban-message: "&c消息包含违规词汇，已拦截！"

# 头衔格式（{title} 会被替换为头衔内容）
title-format: "&7[&r{title}&7]"

# 默认屏蔽词列表
banned-words:
  - "操你妈"
  - "傻逼"
  # ... 更多
```
