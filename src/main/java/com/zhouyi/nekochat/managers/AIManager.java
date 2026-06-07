package com.zhouyi.nekochat.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zhouyi.nekochat.NekoChat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AIManager {

    private final NekoChat plugin;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    // 每个玩家的冷却时间（毫秒）
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private boolean enabled;
    private String apiUrl;
    private String apiKey;
    private String model;
    private String systemPrompt;
    private int maxTokens;
    private int timeoutSeconds;
    private int cooldownSeconds;
    private boolean publicReply;
    private String displayName;
    // 自定义提示词: 名称 -> 提示词内容
    private final Map<String, String> customPrompts = new HashMap<>();

    public AIManager(NekoChat plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        reload();
    }

    /**
     * 从配置文件重新加载 AI 设置
     */
    public void reload() {
        var config = plugin.getConfig().getConfigurationSection("ai");
        if (config == null) {
            enabled = false;
            return;
        }

        enabled = config.getBoolean("enabled", false);
        apiUrl = config.getString("api-url", "");
        apiKey = config.getString("api-key", "");
        model = config.getString("model", "Qwen/Qwen2.5-7B-Instruct");
        systemPrompt = config.getString("system-prompt", "你是 NekoChat AI，一个 Minecraft 服务器中的智能助手。请用中文回复，回答简洁友好。");
        maxTokens = config.getInt("max-tokens", 512);
        timeoutSeconds = config.getInt("timeout-seconds", 30);
        cooldownSeconds = config.getInt("cooldown-seconds", 20);
        publicReply = config.getBoolean("public-reply", true);
        displayName = config.getString("display-name", "&b[AI助手]");

        // 加载自定义提示词
        customPrompts.clear();
        var promptsSection = config.getConfigurationSection("prompts");
        if (promptsSection != null) {
            for (String key : promptsSection.getKeys(false)) {
                String prompt = promptsSection.getString(key);
                if (prompt != null && !prompt.isEmpty()) {
                    customPrompts.put(key.toLowerCase(), prompt);
                }
            }
        }

        if (enabled && (apiUrl.isEmpty() || apiKey.isEmpty())) {
            plugin.getLogger().warning("§e[NekoChat] AI 已启用但 API URL 或 API Key 为空，AI 功能不可用！");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查玩家是否在冷却中
     */
    public boolean isOnCooldown(UUID playerUuid) {
        Long lastUse = cooldowns.get(playerUuid);
        if (lastUse == null) return false;
        return (System.currentTimeMillis() - lastUse) < (cooldownSeconds * 1000L);
    }

    /**
     * 获取冷却剩余秒数
     */
    public int getCooldownRemaining(UUID playerUuid) {
        Long lastUse = cooldowns.get(playerUuid);
        if (lastUse == null) return 0;
        long remaining = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastUse);
        return (int) Math.max(0, remaining / 1000);
    }

    /**
     * 获取 AI 显示名称（已解析颜色代码）
     */
    public String getDisplayName() {
        return displayName;
    }

    public boolean isPublicReply() {
        return publicReply;
    }

    /**
     * 获取自定义提示词列表（所有已注册的提示词名）
     */
    public Set<String> getPromptNames() {
        return customPrompts.keySet();
    }

    /**
     * 根据名称获取自定义提示词内容
     *
     * @param name 提示词名称（不区分大小写）
     * @return 提示词内容，不存在返回 null
     */
    public String getPrompt(String name) {
        return customPrompts.get(name.toLowerCase());
    }

    /**
     * 检查玩家消息是否以 @ai 或 @AI 开头
     */
    public static boolean isAiMention(String message) {
        String trimmed = message.trim().toLowerCase();
        return trimmed.startsWith("@ai") || trimmed.startsWith("@ai ");
    }

    /**
     * 从 @ai 开头的消息中提取实际内容
     * 支持: @ai <消息> 或 @ai <提示词> <消息>
     *
     * @param rawMessage 原始消息（如 "@ai 翻译 你好"）
     * @return 数组 [提示词名, 实际消息] 或 [null, 实际消息]
     */
    public static String[] parseMentionMessage(String rawMessage) {
        String content = rawMessage.trim().substring(3).trim(); // 去掉 @ai
        if (content.isEmpty()) return new String[]{null, ""};

        // 检查第一个词是否是自定义提示词
        // 这个需要从实例获取，所以这里只解析，不判断
        return new String[]{null, content};
    }

    /**
     * 向 AI 发送消息并获取回复（异步）- 使用自定义提示词
     *
     * @param playerName    玩家名
     * @param message       消息内容
     * @param customPrompt  自定义系统提示词（null 则使用默认）
     * @return AI 回复文本
     */
    public CompletableFuture<String> askAI(String playerName, String message, String customPrompt) {
        if (!enabled || apiUrl.isEmpty() || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture("&cAI 功能未配置或已禁用！");
        }

        String effectivePrompt = (customPrompt != null) ? customPrompt : systemPrompt;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();

        if (effectivePrompt != null && !effectivePrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", effectivePrompt);
            messages.add(systemMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", maxTokens);

        String jsonBody = gson.toJson(requestBody);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();

                        if (plugin.isDebug()) {
                            plugin.getLogger().info("§7[NekoChat-DEBUG] AI API 响应状态: " + statusCode);
                        }

                        if (statusCode != 200) {
                            plugin.getLogger().warning("§c[NekoChat] AI API 请求失败 [" + statusCode + "]: " + responseBody);
                            return "&cAI 请求失败，状态码: " + statusCode;
                        }

                        try {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                            JsonArray choices = jsonResponse.getAsJsonArray("choices");
                            if (choices != null && choices.size() > 0) {
                                JsonObject choice = choices.get(0).getAsJsonObject();
                                JsonObject respMsg = choice.getAsJsonObject("message");
                                if (respMsg != null && respMsg.get("content") != null) {
                                    String content = respMsg.get("content").getAsString();
                                    if (plugin.getDatabaseManager().isEnabled()) {
                                        plugin.getDatabaseManager().logAIInteraction(playerName, message, content);
                                    }
                                    return content;
                                }
                            }
                            plugin.getLogger().warning("§c[NekoChat] AI API 返回格式异常: " + responseBody);
                            return "&cAI 返回格式异常，请稍后再试。";
                        } catch (Exception e) {
                            plugin.getLogger().severe("§c[NekoChat] 解析 AI 响应失败: " + e.getMessage());
                            return "&c解析 AI 响应失败: " + e.getMessage();
                        }
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().severe("§c[NekoChat] AI API 请求异常: " + e.getMessage());
                        return "&cAI 请求异常: " + e.getMessage();
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture("&cAI 请求发送失败: " + e.getMessage());
        }
    }

    /**
     * 向 AI 发送消息并获取回复（异步）- 使用默认系统提示词
     */
    public CompletableFuture<String> askAI(String playerName, String message) {
        return askAI(playerName, message, null);
    }

    /**
     * 记录玩家使用 AI 的时间（更新冷却）
     */
    public void recordUsage(UUID playerUuid) {
        cooldowns.put(playerUuid, System.currentTimeMillis());
    }
}
