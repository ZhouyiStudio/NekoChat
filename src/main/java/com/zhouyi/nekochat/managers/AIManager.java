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
        cooldownSeconds = config.getInt("cooldown-seconds", 5);
        publicReply = config.getBoolean("public-reply", true);
        displayName = config.getString("display-name", "&b[AI助手]");

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
     * 向 AI 发送消息并获取回复（异步）
     *
     * @param playerName 玩家名（用于记录）
     * @param message    消息内容
     * @return AI 回复文本的 CompletableFuture
     */
    public CompletableFuture<String> askAI(String playerName, String message) {
        if (!enabled || apiUrl.isEmpty() || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture("&cAI 功能未配置或已禁用！");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();

        // 系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 用户消息
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", maxTokens);

        String jsonBody = gson.toJson(requestBody);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            HttpRequest request = requestBuilder.build();

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
                                    // 记录到数据库
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
     * 记录玩家使用 AI 的时间（更新冷却）
     */
    public void recordUsage(UUID playerUuid) {
        cooldowns.put(playerUuid, System.currentTimeMillis());
    }
}
