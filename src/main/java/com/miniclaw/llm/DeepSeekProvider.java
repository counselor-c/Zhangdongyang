package com.miniclaw.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miniclaw.config.ConfigManager;
import com.miniclaw.context.Message;
import com.miniclaw.tool.Tool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class DeepSeekProvider implements LLMProvider {
    private final String url;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;

    public DeepSeekProvider() {
        ConfigManager config = ConfigManager.getInstance();
        this.url = config.getProperty("llm.deepseek.url");
        this.apiKey = config.getProperty("llm.deepseek.apiKey");
        this.model = config.getProperty("llm.deepseek.model");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    @Override
    public String chat(String prompt) {
        return "Not implemented directly. Use chatWithTools.";
    }

    @Override
    public LLMResponse chatWithTools(List<Message> context, List<Tool> availableTools, Consumer<String> streamCallback) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messagesArray = new JsonArray();
            for (Message msg : context) {
                JsonObject msgObj = new JsonObject();
                // 解决 DeepSeek 对上下文角色的严格校验（不支持独立的 tool 角色或者缺少 tool_call_id 的情况）
                if ("tool".equals(msg.getRole())) {
                    msgObj.addProperty("role", "user");
                    msgObj.addProperty("content", "Tool execution result:\n" + msg.getContent() + "\n\nPlease summarize the result to answer my previous request.");
                } else {
                    msgObj.addProperty("role", msg.getRole());
                    msgObj.addProperty("content", msg.getContent());
                }
                messagesArray.add(msgObj);
            }
            requestBody.add("messages", messagesArray);

            if (availableTools != null && !availableTools.isEmpty()) {
                JsonArray toolsArray = new JsonArray();
                for (Tool tool : availableTools) {
                    JsonObject toolObj = new JsonObject();
                    toolObj.addProperty("type", "function");
                    JsonObject functionObj = new JsonObject();
                    functionObj.addProperty("name", tool.getName());
                    functionObj.addProperty("description", tool.getDescription());
                    functionObj.add("parameters", gson.fromJson(tool.getParametersSchema(), JsonObject.class));
                    toolObj.add("function", functionObj);
                    toolsArray.add(toolObj);
                }
                requestBody.add("tools", toolsArray);
            }

            requestBody.addProperty("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
                JsonArray choices = responseObj.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject messageObj = choices.get(0).getAsJsonObject().getAsJsonObject("message");

                    if (messageObj.has("tool_calls") && !messageObj.get("tool_calls").isJsonNull()) {
                        JsonArray toolCalls = messageObj.getAsJsonArray("tool_calls");
                        if (toolCalls.size() > 0) {
                            JsonObject toolCall = toolCalls.get(0).getAsJsonObject().getAsJsonObject("function");
                            return new LLMResponse(toolCall.get("name").getAsString(), toolCall.get("arguments").getAsString());
                        }
                    }

                    JsonElement contentElem = messageObj.get("content");
                    String content = (contentElem != null && !contentElem.isJsonNull()) ? contentElem.getAsString() : "";
                    
                    if (streamCallback != null && !content.isEmpty()) {
                        // 模拟流式输出
                        for (char c : content.toCharArray()) {
                            streamCallback.accept(String.valueOf(c));
                            try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                        }
                    }
                    return new LLMResponse(content);
                }
                throw new RuntimeException("No choices in response: " + response.body());
            } else {
                throw new RuntimeException("HTTP Error: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            System.err.println("\nDeepSeek LLM call failed: " + e.getMessage());
            e.printStackTrace();
            return new LLMResponse("Sorry, I encountered an error: " + e.getMessage());
        }
    }
}
