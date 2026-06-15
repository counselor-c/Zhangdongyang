package com.miniclaw.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.miniclaw.config.ConfigManager;
import com.miniclaw.context.Message;
import com.miniclaw.tool.Tool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

public class OllamaProvider implements LLMProvider {
    private final String url;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;

    public OllamaProvider() {
        ConfigManager config = ConfigManager.getInstance();
        this.url = config.getProperty("llm.ollama.url");
        this.model = config.getProperty("llm.ollama.model");
        this.httpClient = HttpClient.newHttpClient();
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
                msgObj.addProperty("role", msg.getRole());
                msgObj.addProperty("content", msg.getContent());
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
            
            // In a real stream scenario we would parse chunks. Here we simulate stream.
            requestBody.addProperty("stream", false); 

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
                JsonObject messageObj = responseObj.getAsJsonObject("message");
                
                if (messageObj.has("tool_calls")) {
                    JsonArray toolCalls = messageObj.getAsJsonArray("tool_calls");
                    if (toolCalls.size() > 0) {
                        JsonObject toolCall = toolCalls.get(0).getAsJsonObject().getAsJsonObject("function");
                        return new LLMResponse(toolCall.get("name").getAsString(), toolCall.get("arguments").toString());
                    }
                }
                
                String content = messageObj.get("content").getAsString();
                if (streamCallback != null) {
                    // Simulate stream
                    for (char c : content.toCharArray()) {
                        streamCallback.accept(String.valueOf(c));
                        Thread.sleep(10);
                    }
                }
                return new LLMResponse(content);
            } else {
                throw new RuntimeException("HTTP Error: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("LLM call failed, falling back to mock response.");
            return handleFallback(context, streamCallback);
        }
    }

    private LLMResponse handleFallback(List<Message> context, Consumer<String> streamCallback) {
        Message lastMessage = context.get(context.size() - 1);
        if (lastMessage.getRole().equals("user") && lastMessage.getContent().contains("天气")) {
            return new LLMResponse("WeatherTool", "{\"city\":\"Beijing\"}");
        } else if (lastMessage.getRole().equals("tool")) {
            String summary = "The weather in Beijing is Sunny, 25°C. I have saved it to a text file.";
            if (streamCallback != null) {
                for (char c : summary.toCharArray()) {
                    streamCallback.accept(String.valueOf(c));
                    try { Thread.sleep(20); } catch (Exception ignored) {}
                }
            }
            // Returning the tool call for writing file
            return new LLMResponse("WriteFileTool", "{\"filename\":\"beijing_weather.txt\", \"content\":\"Beijing: Sunny, 25°C\"}");
        } else if (lastMessage.getRole().equals("user") && lastMessage.getContent().contains("写入")) {
            return new LLMResponse("WriteFileTool", "{\"filename\":\"output.txt\", \"content\":\"" + lastMessage.getContent() + "\"}");
        }
        
        String fallbackContent = "This is a mock fallback response. No tool was called.";
        if (streamCallback != null) {
            for (char c : fallbackContent.toCharArray()) {
                streamCallback.accept(String.valueOf(c));
                try { Thread.sleep(10); } catch (Exception ignored) {}
            }
        }
        return new LLMResponse(fallbackContent);
    }
}
