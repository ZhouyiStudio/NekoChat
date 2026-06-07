# NekoChat

一个适用于 **Purpur 1.21.1** (Paper API) 的全功能聊天管理插件，支持头衔、屏蔽词、禁言、服务器宣传检测、AI 助手、自定义管理员和消息自定义。

## 功能

### 🤖 AI 助手（v1.0.29+）
```
/ai <消息>                   向 AI 提问
/ai <提示词> <消息>          使用指定提示词提问
@ai <消息>                   在聊天框直接 @ai 触发 AI
@ai <提示词> <消息>          使用指定提示词 + @ai 触发
```
- 支持**任何 OpenAI 兼容 API**（ModelScope、OpenAI、Azure 等）
- 可在 `config.yml` 中配置 API 地址、Key、模型、系统提示词
- **冷却保护**（默认 20 秒）、**每日次数限制**（默认 10 次/天）
- **公聊/私聊模式**切换：回复全服可见或仅提问者可见
- **自定义提示词预设**：如 `/ai 翻译 你好` 使用翻译模式
- **`@AI` 绿色高亮**显示在聊天中

### 🚫 AI 封禁
```
/aiban <玩家>             禁止玩家使用 AI
/aiban remove <玩家>      解除 AI 封禁
/aiban list               查看被封禁玩家列表
```
- 被封禁的玩家无法使用 `/ai` 和 `@ai`
- 仅管理员（`/nchat op add`）可操作
- 数据持久化到 `aibans.yml`

### 🏷️ 头衔系统
```
/ptitle add <玩家> <头衔>    给指定玩家设置头衔（支持 & 颜色代码）
/ptitle remove <玩家>       移除指定玩家头衔
```
- 头衔显示在 **聊天消息** 和 **Tab 列表** 中
- 格式: `[头衔] <玩家名> 消息内容`

### 👑 自定义管理员系统
```
/nchat op add <玩家>        添加管理员
/nchat op remove <玩家>     移除管理员
/nchat op list              查看管理员列表
```
- 只有通过 `/nchat op add` 添加的玩家才能使用管理命令
- 控制台（服务器后台）始终有管理权限
- 首次使用在后台执行: `nchat op add <你的游戏名>`
- Bukkit OP 玩家也需要添加才能使用

### 🚫 屏蔽词系统
```
/cban add <词汇>     添加屏蔽词
/cban remove <词汇>  移除屏蔽词
/cban list           查看所有屏蔽词
```
- 默认内置中英文常见屏蔽词
- 含屏蔽词的消息会被自动拦截
- 宣传白名单中的词汇同时放行屏蔽词检测

### ✅ 宣传检测白名单
```
/cwhitelist add <词汇>     添加宣传白名单
/cwhitelist remove <词汇>  移除宣传白名单
/cwhitelist list           查看白名单
```
- 白名单内的域名/关键词不会被宣传检测拦截
- 白名单也同时放行屏蔽词检测

### 🔇 禁言系统
```
/mute <玩家> [时间]   禁言玩家（不写时间 = 永久）
/unmute <玩家>         解除禁言
```
- 时间格式: `30s` / `5m` / `2h` / `1d`
- 到期自动解除

### 🔗 URL 自动解析
- 聊天中的 URL 自动变为**天蓝色可点击文本**
- 鼠标悬停提示，左键直接打开浏览器
- 支持 `https://...` 和 `www.xxx.com` 格式

### 🛡️ 服务器宣传检测
- 自动拦截聊天中的 IP 地址和外站域名
- 仅在白名单内的服务器可通过（默认 `3d3k.org`、`3d3k.discourse.group`）
- 可在 `config.yml` 中关闭

### 💬 消息自定义（v1.0.29+）
- 所有插件消息（130+ 条）均可在 `messages.yml` 中自定义
- 支持 `&` 颜色代码和 `{0} {1}` 占位符
- 修改后执行 `/nchat reload` 重载

### 🗄️ 数据库记录
- 支持 MySQL 记录：公聊、私聊、/say、AI 问答
- 表结构 `nc_chat_logs`，按类型区分（chat / whisper / system / ai）
- 默认关闭，配置后自动创建表

### 📋 控制台命令
```
/nchat reload             重载所有配置（含 AI 和消息）
/nchat reload ai          仅重载 AI 配置
```

## 快速开始

### 1. 下载
从 [Releases](https://github.com/ZhouyiStudio/NekoChat/releases) 下载最新 jar。

### 2. 安装
放入 `plugins/` 目录，重启服务器。

### 3. 初始化管理员
```bash
# 在服务器后台执行
nchat op add <你的游戏名>
```

### 4. 配置 AI（可选）
编辑 `plugins/NekoChat/config.yml`：

```yaml
ai:
  enabled: true
  api-url: "https://api-inference.modelscope.cn/v1/chat/completions"
  api-key: "你的API密钥"
  model: "Qwen/Qwen2.5-7B-Instruct"
```

### 5. 重载配置
```
/nchat reload
```

## 构建

### 环境要求
- JDK 21
- Gradle 8.7+

### 构建步骤

```bash
git clone https://github.com/ZhouyiStudio/NekoChat.git
cd NekoChat
./gradlew build
# 产物在 build/libs/NekoChat-*.jar
```

> 版本号基于 Git 提交数自动生成：`1.0.<提交数>`

## 配置

### config.yml

```yaml
# 调试模式
debug: false

# 头衔格式
title-format: "&7[&r{title}&7]"

# 数据库（不配置则不启用）
database:
  host: localhost
  port: 3306
  database: nekochat
  user: root
  password: ""

# AI 助手
ai:
  enabled: false
  api-url: "https://api-inference.modelscope.cn/v1/chat/completions"
  api-key: ""
  model: "Qwen/Qwen2.5-7B-Instruct"
  system-prompt: "你是 NekoChat AI..."
  max-tokens: 512
  timeout-seconds: 30
  cooldown-seconds: 20
  daily-limit: 10
  public-reply: true
  display-name: "&b[AI助手]"
  prompts:
    翻译: "你是一个翻译助手..."
    笑话: "你是一个讲笑话的 AI..."
    代码: "你是编程专家..."

# 屏蔽词
cban-message: "&c消息包含违规词汇，已拦截！"
banned-words:
  - "fuck"
  - "shit"
  # ...

# 宣传检测
server-ad-check: true
server-ad-message: "&c请勿宣传其他服务器！"
server-ad-whitelist:
  - "3d3k.org"
```

### messages.yml
所有插件消息可在 `plugins/NekoChat/messages.yml` 中自定义颜色和文本，修改后 `/nchat reload` 生效。

## 许可

MIT License
