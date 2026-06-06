# NekoChat

一个适用于 Purpur 1.21.1 (Paper API) 的聊天管理插件，支持头衔、屏蔽词、禁言、服务器宣传检测和自定义管理员系统。

## 功能

### 🏷️ 头衔系统
```
/ptitle add <玩家> <头衔>    给指定玩家设置头衔（支持 & 颜色代码）
/ptitle remove <玩家>      移除指定玩家头衔
```
- 头衔显示在 **聊天消息** 和 **Tab 列表** 中
- 格式: `[头衔] <玩家名> 消息内容`

### 📖 帮助
```
/nchat                      显示插件帮助信息
/nchat reload               重新加载配置文件
```

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

### ✅ 宣传检测白名单
```
/cwhitelist add <词汇>     添加宣传白名单
/cwhitelist remove <词汇>  移除宣传白名单
/cwhitelist list           查看白名单
```
- 默认白名单: `3d3k.org`, `3d3k.discourse.group`
- 白名单内的域名/关键词不会被宣传检测拦截

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
- 仅允许白名单内的服务器（默认 `3d3k.org`、`3d3k.discourse.group`）
- 可在 `config.yml` 中关闭

## 构建

### 环境要求
- JDK 21
- Gradle 8.7+

### 构建步骤

```bash
git clone https://github.com/ZhouyiStudio/NekoChat.git
cd NekoChat
./gradlew build
# 产物在 build/libs/NekoChat-*.jar（版本号自动迭代）
```

> 版本号基于 Git 提交数自动生成：`1.0.<提交数>`

## 配置

`plugins/NekoChat/config.yml`:

```yaml
# 头衔格式
title-format: "&7[&r{title}&7]"

# 禁言提示
mute-message: "&c你已被禁言{reason}&c！"

# 屏蔽词提示
cban-message: "&c消息包含违规词汇，已拦截！"

# 默认屏蔽词列表
banned-words:
  - "操你妈"
  - "傻逼"
  # ...

# 服务器宣传检测
server-ad-check: true
server-ad-message: "&c请勿宣传其他服务器！"
server-ad-whitelist:
  - "3d3k.org"
  - "3d3k.discourse.group"
```
